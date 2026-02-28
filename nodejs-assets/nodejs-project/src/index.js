const {
    default: makeWASocket,
    useMultiFileAuthState,
    fetchLatestBaileysVersion,
    DisconnectReason,
    downloadContentFromMessage,
    makeCacheableSignalKeyStore,
    getContentType
} = require('@whiskeysockets/baileys');

const P = require('pino');
const { Boom } = require('@hapi/boom');
const { WebSocketServer } = require('ws');
const fs = require('fs');
const path = require('path');

const rn = require('rn-bridge');

const DATA_DIR = rn.app.datadir();
const AUTH_DIR = path.join(DATA_DIR, 'auth_limax');
const MEDIA_DIR = path.join(DATA_DIR, 'media');
const SETTINGS_FILE = path.join(DATA_DIR, 'settings.json');
const MEDIA_LOG = path.join(DATA_DIR, 'media_log.json');
const DELETED_LOG = path.join(DATA_DIR, 'deleted_log.json');

[AUTH_DIR, MEDIA_DIR].forEach(d => {
    if (!fs.existsSync(d)) fs.mkdirSync(d, { recursive: true });
});

function loadJson(f, d) {
    try { if (fs.existsSync(f)) return JSON.parse(fs.readFileSync(f, 'utf-8')); } catch (_) {}
    return d;
}
function saveJson(f, d) { try { fs.writeFileSync(f, JSON.stringify(d, null, 2)); } catch (_) {} }

let settings = loadJson(SETTINGS_FILE, {
    mediaPrefix: '!salvar',
    notifyOnDownload: true,
    antiDelete: true,
    phoneNumber: ''
});
let savedMedia = loadJson(MEDIA_LOG, []);
let deletedMessages = loadJson(DELETED_LOG, []);

let sock = null;
let isConnected = false;

function send(type, data) {
    rn.channel.send(JSON.stringify({ type, data, ts: Date.now() }));
}

rn.channel.on('message', async (raw) => {
    try {
        const { action, payload } = JSON.parse(raw);
        switch (action) {
            case 'start_bot':
                settings.phoneNumber = payload.phoneNumber;
                saveJson(SETTINGS_FILE, settings);
                await startBot();
                break;
            case 'disconnect':
                if (sock) { try { await sock.logout(); } catch (_) {} }
                break;
            case 'update_settings':
                settings = { ...settings, ...payload };
                saveJson(SETTINGS_FILE, settings);
                send('settings_updated', settings);
                break;
            case 'get_media':
                send('media_list', savedMedia);
                break;
            case 'get_deleted':
                send('deleted_list', deletedMessages);
                break;
            case 'get_contact_info':
                await fetchContact(payload.number);
                break;
            case 'get_state':
                send('state', { connected: isConnected, settings, mediaCount: savedMedia.length, deletedCount: deletedMessages.length });
                break;
        }
    } catch (e) {
        send('error', { message: e.message });
    }
});

async function fetchContact(number) {
    if (!sock || !isConnected) { send('contact_info', null); return; }
    try {
        const jid = `${number.replace(/\D/g, '')}@s.whatsapp.net`;
        const [status, pic] = await Promise.allSettled([
            sock.fetchStatus(jid),
            sock.profilePictureUrl(jid, 'image')
        ]);
        send('contact_info', {
            number, jid,
            status: status.status === 'fulfilled' ? status.value?.status : null,
            profilePic: pic.status === 'fulfilled' ? pic.value : null,
            fetchedAt: Date.now()
        });
    } catch (e) {
        send('contact_info', null);
    }
}

async function startBot() {
    try {
        const { state, saveCreds } = await useMultiFileAuthState(AUTH_DIR);
        const { version } = await fetchLatestBaileysVersion();

        sock = makeWASocket({
            version,
            logger: P({ level: 'silent' }),
            printQRInTerminal: false,
            auth: {
                creds: state.creds,
                keys: makeCacheableSignalKeyStore(state.keys, P({ level: 'fatal' }).child({ level: 'fatal' }))
            },
            browser: ['LimaxBot', 'Chrome', '120.0.0'],
            markOnlineOnConnect: false,
            getMessage: async () => ({ conversation: '' })
        });

        if (!sock.authState.creds.registered && settings.phoneNumber) {
            setTimeout(async () => {
                try {
                    const code = await sock.requestPairingCode(settings.phoneNumber.replace(/\D/g, ''));
                    send('pairing_code', { code: code?.match(/.{1,4}/g)?.join('-') || code });
                } catch (e) {
                    send('error', { message: 'Erro ao gerar código: ' + e.message });
                }
            }, 3000);
        }

        sock.ev.on('creds.update', saveCreds);

        sock.ev.on('connection.update', async (update) => {
            const { connection, lastDisconnect } = update;
            if (connection === 'open') {
                isConnected = true;
                send('connection', { status: 'connected', connectedAt: Date.now() });
            }
            if (connection === 'close') {
                isConnected = false;
                const code = new Boom(lastDisconnect?.error)?.output?.statusCode;
                const reconnect = code !== DisconnectReason.loggedOut;
                send('connection', { status: 'disconnected', reason: lastDisconnect?.error?.message || 'Conexão perdida', reconnecting: reconnect });
                if (reconnect) setTimeout(() => startBot(), 5000);
            }
        });

        sock.ev.on('messages.upsert', async ({ messages, type }) => {
            if (type !== 'notify') return;
            for (const msg of messages) {
                if (!msg.message || msg.key.fromMe) continue;
                const from = msg.key.remoteJid;
                const isGroup = from?.endsWith('@g.us') || false;
                const sender = isGroup ? msg.key.participant : from;
                const bodyText = msg.message?.conversation || msg.message?.extendedTextMessage?.text || msg.message?.imageMessage?.caption || msg.message?.videoMessage?.caption || '';
                if (settings.mediaPrefix && bodyText.trim().toLowerCase().startsWith(settings.mediaPrefix.trim().toLowerCase())) {
                    const quoted = msg.message?.extendedTextMessage?.contextInfo?.quotedMessage;
                    if (quoted) await handleMediaDownload(quoted, from, sender, isGroup);
                }
            }
        });

        sock.ev.on('messages.delete', (item) => {
            if (!settings.antiDelete) return;
            const keys = 'keys' in item ? item.keys : [];
            for (const key of keys) {
                const entry = { id: key.id, from: key.remoteJid, participant: key.participant, deletedAt: Date.now(), isGroup: key.remoteJid?.endsWith('@g.us') || false };
                deletedMessages.unshift(entry);
                if (deletedMessages.length > 500) deletedMessages = deletedMessages.slice(0, 500);
                saveJson(DELETED_LOG, deletedMessages);
                send('message_deleted', entry);
            }
        });

    } catch (e) {
        send('error', { message: 'Erro ao iniciar bot: ' + e.message });
    }
}

async function handleMediaDownload(quoted, from, sender, isGroup) {
    try {
        let viewOnceMsg = quoted.viewOnceMessageV2?.message || quoted.viewOnceMessage?.message || quoted;
        let mediaType = getContentType(viewOnceMsg);
        if (!mediaType || mediaType === 'messageContextInfo') {
            mediaType = Object.keys(viewOnceMsg).filter(k => k !== 'messageContextInfo')[0];
        }
        let streamType;
        if (mediaType === 'imageMessage') streamType = 'image';
        else if (mediaType === 'videoMessage') streamType = 'video';
        else if (mediaType === 'audioMessage') streamType = 'audio';
        else return;

        send('download_start', { streamType, from, sender });

        const stream = await downloadContentFromMessage(viewOnceMsg[mediaType], streamType);
        const chunks = [];
        for await (const chunk of stream) chunks.push(chunk);
        const buffer = Buffer.concat(chunks);

        const ext = streamType === 'image' ? 'jpg' : streamType === 'video' ? 'mp4' : 'mp3';
        const filename = `media_${Date.now()}.${ext}`;
        const filepath = path.join(MEDIA_DIR, filename);
        fs.writeFileSync(filepath, buffer);

        const entry = {
            id: Date.now(),
            filename,
            filepath,
            type: streamType,
            from, sender, isGroup,
            size: buffer.length,
            downloadedAt: Date.now(),
            preview: streamType === 'image' ? `data:image/jpeg;base64,${buffer.slice(0, 60000).toString('base64')}` : null
        };
        savedMedia.unshift(entry);
        if (savedMedia.length > 200) savedMedia = savedMedia.slice(0, 200);
        saveJson(MEDIA_LOG, savedMedia);
        send('media_downloaded', entry);
    } catch (e) {
        send('error', { message: 'Erro ao baixar mídia: ' + e.message });
    }
}
