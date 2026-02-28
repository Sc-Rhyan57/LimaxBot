package com.limaxbot.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.limaxbot.ui.components.*
import com.limaxbot.ui.theme.LC
import com.limaxbot.viewmodel.BotViewModel

@Composable
fun SettingsScreen(vm: BotViewModel) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    var prefix by remember(state.settings.mediaPrefix) { mutableStateOf(state.settings.mediaPrefix) }
    var notifyDl by remember(state.settings.notifyOnDownload) { mutableStateOf(state.settings.notifyOnDownload) }
    var antiDel by remember(state.settings.antiDelete) { mutableStateOf(state.settings.antiDelete) }
    var saved by remember { mutableStateOf(false) }

    LaunchedEffect(saved) { if (saved) { kotlinx.coroutines.delay(2000); saved = false } }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(LC.Bg, LC.BgDark))),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Configurações", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = LC.White)
            Text("Personalize o comportamento do bot", fontSize = 12.sp, color = LC.Muted)
        }

        item {
            LCard("Prefixo de Mídia", Icons.Outlined.Tag) {
                LField("Prefixo para detectar e baixar mídias", prefix, { prefix = it }, "Ex: !salvar, .dl, \\b, !")
                Text(
                    "Responda uma foto/vídeo com o prefixo e o bot baixa automaticamente. Pode ser qualquer texto.",
                    fontSize = 11.sp, color = LC.Muted,
                    modifier = Modifier.fillMaxWidth().background(LC.Green.copy(0.05f), RoundedCornerShape(8.dp)).border(1.dp, LC.Green.copy(0.15f), RoundedCornerShape(8.dp)).padding(10.dp)
                )
            }
        }

        item {
            LCard("Notificações", Icons.Outlined.Notifications) {
                ToggleRow(Icons.Outlined.Download, "Notificar ao baixar mídia", "Exibe 'Baixando arquivo de mídia...' ao detectar o prefixo", notifyDl) { notifyDl = it }
            }
        }

        item {
            LCard("Anti-Delete", Icons.Outlined.Shield) {
                ToggleRow(Icons.Outlined.DeleteSweep, "Capturar mensagens apagadas", "Salva mensagens excluídas automaticamente na aba 'Apagadas'", antiDel) { antiDel = it }
            }
        }

        item {
            AnimatedVisibility(visible = saved, enter = fadeIn() + slideInVertically(), exit = fadeOut()) {
                Row(Modifier.fillMaxWidth().background(LC.Green.copy(0.08f), RoundedCornerShape(10.dp)).border(1.dp, LC.Green.copy(0.25f), RoundedCornerShape(10.dp)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Outlined.CheckCircle, null, tint = LC.Green, modifier = Modifier.size(16.dp))
                    Text("Configurações salvas!", color = LC.Green, fontSize = 13.sp)
                }
            }
            LButton("Salvar Configurações", onClick = {
                vm.saveSettings(state.settings.copy(mediaPrefix = prefix, notifyOnDownload = notifyDl, antiDelete = antiDel))
                saved = true
            }, icon = Icons.Outlined.Save)
        }

        item {
            LCard("Sobre", Icons.Outlined.Info) {
                InfoRow("Versão", "2.0.0")
                InfoRow("Desenvolvedor", "Rhyan57")
                InfoRow("Engine", "Node.js + Baileys (embutido)")
                InfoRow("Android", "8.0+ (API 26+)")
                OutlinedButton(
                    onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Sc-Rhyan57/McpeBot"))) },
                    modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, LC.Border), shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Outlined.Code, null, tint = LC.Muted, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Sc-Rhyan57/McpeBot", color = LC.Muted, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun ToggleRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onChanged: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(36.dp).background(LC.Green.copy(0.1f), RoundedCornerShape(10.dp)).border(1.dp, LC.Green.copy(0.2f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = LC.Green, modifier = Modifier.size(17.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = LC.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = LC.Muted, fontSize = 11.sp)
        }
        Switch(checked = checked, onCheckedChange = onChanged, colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White, checkedTrackColor = LC.Green,
            uncheckedThumbColor = LC.Muted, uncheckedTrackColor = LC.SurfaceVar
        ))
    }
}
