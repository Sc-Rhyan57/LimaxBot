package com.limaxbot.ui.screens

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
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.limaxbot.model.DeletedEntry
import com.limaxbot.ui.components.*
import com.limaxbot.ui.theme.LC
import com.limaxbot.viewmodel.BotViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DeletedScreen(vm: BotViewModel) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { vm.refreshDeleted() }

    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(LC.Bg, LC.BgDark)))) {
        Row(Modifier.fillMaxWidth().padding(16.dp, 16.dp, 16.dp, 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Mensagens Apagadas", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = LC.White)
                Text("${state.deletedList.size} capturadas", fontSize = 12.sp, color = LC.Muted)
            }
            IconButton(onClick = { vm.refreshDeleted() }, modifier = Modifier.size(38.dp).background(LC.CardAlt, RoundedCornerShape(10.dp))) {
                Icon(Icons.Outlined.Refresh, null, tint = LC.Green, modifier = Modifier.size(18.dp))
            }
        }

        if (!state.settings.antiDelete) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    .background(LC.Warning.copy(0.08f), RoundedCornerShape(10.dp))
                    .border(1.dp, LC.Warning.copy(0.3f), RoundedCornerShape(10.dp)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.WarningAmber, null, tint = LC.Warning, modifier = Modifier.size(16.dp))
                Text("Anti-delete desativado. Ative nas Configurações.", color = LC.Warning, fontSize = 12.sp)
            }
        }

        if (state.deletedList.isEmpty()) {
            EmptyState(Icons.Outlined.DeleteSweep, "Nenhuma mensagem apagada", "Quando alguém apagar uma mensagem ela aparecerá aqui")
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.deletedList, key = { "${it.id}_${it.deletedAt}" }) { DeletedCard(it) }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun DeletedCard(e: DeletedEntry) {
    val fmt = SimpleDateFormat("dd/MM/yy HH:mm:ss", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, LC.Error.copy(0.2f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = LC.Error.copy(0.04f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(34.dp).background(LC.Error.copy(0.1f), CircleShape).border(1.dp, LC.Error.copy(0.3f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Delete, null, tint = LC.Error, modifier = Modifier.size(16.dp))
                    }
                    Column {
                        Text(shortenJid(e.from), color = LC.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (e.isGroup && e.participant != null) Text("Por: ${shortenJid(e.participant)}", color = LC.Muted, fontSize = 11.sp)
                    }
                }
                StatusBadge(if (e.isGroup) "Grupo" else "Privado", if (e.isGroup) LC.Info else LC.Green)
            }
            HorizontalDivider(color = LC.Error.copy(0.12f))
            if (e.content != null) {
                Text(e.content, color = LC.White, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().background(LC.CardAlt, RoundedCornerShape(8.dp)).padding(10.dp))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(Icons.Outlined.Info, null, tint = LC.Muted, modifier = Modifier.size(13.dp))
                    Text("Conteúdo não disponível (mídia)", color = LC.Muted, fontSize = 11.sp)
                }
            }
            Text(fmt.format(Date(e.deletedAt)), color = LC.Error.copy(0.6f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.End)
        }
    }
}
