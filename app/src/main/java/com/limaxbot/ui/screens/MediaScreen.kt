package com.limaxbot.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.limaxbot.model.MediaEntry
import com.limaxbot.ui.components.*
import com.limaxbot.ui.theme.LC
import com.limaxbot.viewmodel.BotViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MediaScreen(vm: BotViewModel) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { vm.refreshMedia() }

    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(LC.Bg, LC.BgDark)))) {
        Row(Modifier.fillMaxWidth().padding(16.dp, 16.dp, 16.dp, 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Mídias Salvas", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = LC.White)
                Text("${state.mediaList.size} arquivos", fontSize = 12.sp, color = LC.Muted)
            }
            IconButton(onClick = { vm.refreshMedia() }, modifier = Modifier.size(38.dp).background(LC.CardAlt, RoundedCornerShape(10.dp))) {
                Icon(Icons.Outlined.Refresh, null, tint = LC.Green, modifier = Modifier.size(18.dp))
            }
        }
        if (state.mediaList.isEmpty()) {
            EmptyState(Icons.Outlined.PermMedia, "Nenhuma mídia salva", "Use o prefixo configurado respondendo uma foto/vídeo")
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.mediaList, key = { it.id }) { MediaCard(it) }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun MediaCard(m: MediaEntry) {
    var expanded by remember { mutableStateOf(false) }
    val fmt = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    val typeColor = when (m.type) { "video" -> LC.Info; "audio" -> LC.Warning; else -> LC.Green }

    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, LC.Border, RoundedCornerShape(12.dp)).clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = LC.Card),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(LC.CardAlt), contentAlignment = Alignment.Center) {
                    if (m.type == "image" && m.preview != null) {
                        AsyncImage(model = m.preview, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(when (m.type) { "video" -> Icons.Outlined.VideoFile; "audio" -> Icons.Outlined.AudioFile; else -> Icons.Outlined.Image }, null, tint = typeColor, modifier = Modifier.size(22.dp))
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusBadge(m.type.replaceFirstChar { it.uppercase() }, typeColor)
                        if (m.isGroup) StatusBadge("Grupo", LC.Muted)
                    }
                    Text(shortenJid(m.from), color = LC.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${fmt.format(Date(m.downloadedAt))} · ${fmtSize(m.size)}", color = LC.Muted, fontSize = 11.sp)
                }
                Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = LC.Muted, modifier = Modifier.size(18.dp))
            }
            AnimatedVisibility(expanded) {
                Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    HorizontalDivider(color = LC.Border)
                    Spacer(Modifier.height(2.dp))
                    DetailRow("Arquivo", m.filename)
                    DetailRow("De", m.from)
                    DetailRow("Tamanho", fmtSize(m.size))
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 11.sp, color = LC.Muted)
        Text(value, fontSize = 11.sp, color = LC.White, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false).padding(start = 8.dp))
    }
}

fun shortenJid(jid: String) = jid.replace("@s.whatsapp.net", "").replace("@g.us", " (grupo)")
fun fmtSize(b: Long) = when { b < 1024 -> "$b B"; b < 1048576 -> "${b/1024} KB"; else -> "${b/1048576} MB" }
