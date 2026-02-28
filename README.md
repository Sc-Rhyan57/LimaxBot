# LimaxBot 2.0

App Android completo com **Node.js embutido**. Sem Termux, sem servidor externo.

## ✅ Como compilar via GitHub (sem PC)

1. **Fork** ou faça upload deste repositório no seu GitHub
2. Vá em **Actions** → **Build LimaxBot APK**
3. Clique em **Run workflow**
4. Aguarde ~5 minutos
5. O APK aparece em **Releases** automaticamente pronto para baixar e instalar

## Como usar o app

1. **Instale o APK** no celular
2. Aguarde a tela inicial (o Node.js inicia em ~3 segundos)
3. Digite seu número com DDI: `5541999998888`
4. Toque em **Conectar Bot**
5. Um código aparece na tela — **copie-o**
6. No WhatsApp: **Aparelhos Conectados → Conectar → Conectar com número de telefone**
7. Digite o código — pronto! ✅

## Funcionalidades

| Função | Como funciona |
|--------|--------------|
| **Node.js embutido** | Roda dentro do APK, sem app externo |
| **Pairing Code** | Autentica via código, sem QR |
| **Prefixo customizável** | Configure qualquer prefixo em Configurações |
| **Mídias salvas** | Aba com preview, tipo, origem, tamanho |
| **Anti-delete** | Captura mensagens apagadas |
| **Info de contatos** | Foto de perfil + recado |
| **Notificações** | Conectado, mídia baixando, desconectado |
| **Crash detector** | Tela de erro com log copiável |
| **Tema WhatsApp** | Verde #25D366 + Material 3 Dark |

## Arquitetura

```
APK
├── libnode.so (Node.js nativo para ARM)  ← nodejs-mobile
├── nodejs-project/                        ← assets do bot
│   ├── src/index.js                       ← lógica do bot
│   └── node_modules/                      ← instalados no CI
└── app Kotlin/Compose                     ← interface
    ├── NodeBridge.kt   ← comunica com Node.js via canal interno
    ├── BotViewModel.kt ← estado e lógica
    └── ui/screens/     ← 5 telas separadas
```

*Inspirado em LIMAXBOT • By Rhyan57*
