package com.limaxbot.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.limaxbot.model.BotStatus
import com.limaxbot.ui.components.*
import com.limaxbot.ui.theme.LC
import com.limaxbot.viewmodel.BotViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(vm: BotViewModel) {
    val state by vm.state.collectAsState()
    var phone by remember(state.settings.phoneNumber) { mutableStateOf(state.settings.phoneNumber) }

    // Detectar se o binário node foi encontrado (após nodeReady, se nenhum log de OK apareceu é problema)
    val nodeHasBinary = state.logLines.any {
        it.contains("Binário extraído", true) || it.contains("Processo Node.js iniciado", true)
    }
    val nodeFailed = state.nodeReady && state.logLines.any {
        it.contains("Binário node NÃO encontrado", true) || it.contains("Falha ao iniciar", true)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(LC.Bg, Color(0xFF0D1A0E)))),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("LimaxBot 2.0", fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = LC.White)
                    Text("Painel de Controle", fontSize = 12.sp, color = LC.Muted)
                }
                Box(
                    Modifier.size(44.dp).clip(CircleShape)
                        .background(Brush.radialGradient(listOf(LC.Green.copy(0.25f), LC.GreenDeep.copy(0.05f))))
                        .border(1.dp, LC.Green.copy(0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.SmartToy, null, tint = LC.Green, modifier = Modifier.size(22.dp))
                }
            }
        }

        item { StatusCard(state.status, state.connectedAt, state.mediaList.size, state.deletedList.size, state.nodeReady) }

        // Banner de status do Node.js
        item {
            AnimatedContent(targetState = Triple(state.nodeReady, nodeHasBinary, nodeFailed)) { (ready, hasBinary, failed) ->
                when {
                    // Ainda carregando
                    !ready -> NodeStatusBanner(
                        icon = Icons.Outlined.HourglassBottom,
                        message = "Iniciando engine Node.js...",
                        sub = "Aguarde alguns segundos",
                        color = LC.Warning,
                        loading = true
                    )
                    // Pronto mas binário não encontrado
                    failed -> NodeStatusBanner(
                        icon = Icons.Outlined.ErrorOutline,
                        message = "Binário Node.js não encontrado",
                        sub = "Veja o Console para detalhes. O APK pode precisar ser recompilado.",
                        color = LC.Error,
                        loading = false
                    )
                    // Pronto e funcionando
                    hasBinary -> NodeStatusBanner(
                        icon = Icons.Outlined.CheckCircle,
                        message = "Engine Node.js pronta",
                        sub = "Processo rodando normalmente",
                        color = LC.Green,
                        loading = false
                    )
                    // Pronto mas sem confirmar binário ainda (pode ser que ainda não logou)
                    ready -> NodeStatusBanner(
                        icon = Icons.Outlined.Info,
                        message = "Engine iniciada",
                        sub = "Verifique o Console para detalhes",
                        color = LC.Info,
                        loading = false
                    )
                    else -> {}
                }
            }
        }

        item {
            LCard("Autenticação WhatsApp", Icons.Outlined.Lock) {
                LField(
                    label = "Seu número com DDI (somente números)",
                    value = phone, onValueChange = { phone = it },
                    placeholder = "5541999998888",
                    leadingIcon = Icons.Outlined.Phone,
                    enabled = state.status != BotStatus.CONNECTING && state.nodeReady
                )

                when (state.status) {
                    BotStatus.IDLE, BotStatus.ERROR -> LButton(
                        text = "Conectar Bot",
                        onClick = { if (phone.isNotBlank()) vm.connectBot(phone) },
                        enabled = phone.isNotBlank() && state.nodeReady,
                        icon = Icons.Outlined.QrCode
                    )
                    BotStatus.CONNECTING -> LButton(
                        text = "Aguardando código...", onClick = {}, enabled = false, icon = Icons.Outlined.Sync
                    )
                    BotStatus.CONNECTED -> LButton(
                        text = "Desconectar Bot", onClick = { vm.disconnectBot() },
                        icon = Icons.Outlined.PowerSettingsNew, color = LC.Error
                    )
                }

                AnimatedVisibility(visible = state.pairingCode != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    PairingCodeBox(code = state.pairingCode ?: "")
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun NodeStatusBanner(icon: ImageVector, message: String, sub: String, color: Color, loading: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(color.copy(0.08f), RoundedCornerShape(10.dp))
            .border(1.dp, color.copy(0.3f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = color, strokeWidth = 2.dp)
        } else {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        }
        Column {
            Text(message, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, color = color.copy(0.7f), fontSize = 10.sp)
        }
    }
}

@Composable
fun PairingCodeBox(code: String) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, LC.Green.copy(0.4f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = LC.GreenDeep.copy(0.15f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.Key, null, tint = LC.Green, modifier = Modifier.size(15.dp))
                Text("Código de Emparelhamento", color = LC.Green, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Text(code, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = LC.White, fontFamily = FontFamily.Monospace, letterSpacing = 3.sp)
            Text("WhatsApp → Aparelhos Conectados → Conectar → Conectar com número de telefone", fontSize = 10.sp, color = LC.Muted, textAlign = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { clipboard.setText(AnnotatedString(code)); copied = true },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, LC.Green.copy(0.6f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy, null, tint = LC.Green, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (copied) "Copiado!" else "Copiar Código", color = LC.Green, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun StatusCard(status: BotStatus, connectedAt: Long?, mediaCount: Int, deletedCount: Int, nodeReady: Boolean) {
    val (color, text) = when (status) {
        BotStatus.CONNECTED -> Pair(LC.Green, "Conectado")
        BotStatus.CONNECTING -> Pair(LC.Warning, "Conectando...")
        BotStatus.ERROR -> Pair(LC.Error, "Erro")
        BotStatus.IDLE -> Pair(LC.Muted, "Desconectado")
    }
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, color.copy(0.25f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = color.copy(0.05f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (status == BotStatus.CONNECTED) PulsingDot(color) else Box(Modifier.size(9.dp).clip(CircleShape).background(color))
                Text(text, color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                if (status == BotStatus.CONNECTED) StatusBadge("ONLINE", color)
            }
            if (status == BotStatus.CONNECTED) {
                HorizontalDivider(color = LC.Border.copy(0.4f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatChip("Mídias", "$mediaCount", Icons.Outlined.PermMedia, LC.Green)
                    VerticalDivider(Modifier.height(38.dp), color = LC.Border)
                    StatChip("Apagadas", "$deletedCount", Icons.Outlined.DeleteSweep, LC.Warning)
                    connectedAt?.let {
                        VerticalDivider(Modifier.height(38.dp), color = LC.Border)
                        val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
                        StatChip("Online", fmt.format(Date(it)), Icons.Outlined.AccessTime, LC.Info)
                    }
                }
            }
        }
    }
}

@Composable
fun StatChip(label: String, value: String, icon: ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Text(value, color = LC.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        Text(label, color = LC.Muted, fontSize = 10.sp)
    }
}
