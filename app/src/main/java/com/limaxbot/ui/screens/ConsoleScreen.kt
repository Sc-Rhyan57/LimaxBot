package com.limaxbot.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limaxbot.ui.theme.LC
import com.limaxbot.viewmodel.BotViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConsoleScreen(vm: BotViewModel) {
    val state by vm.state.collectAsState()
    val logs = state.logLines
    val listState = rememberLazyListState()
    val clipboard = LocalClipboardManager.current
    var autoScroll by remember { mutableStateOf(true) }

    // Auto-scroll quando chegar novo log
    LaunchedEffect(logs.size) {
        if (autoScroll && logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(LC.Bg, LC.BgDark)))
    ) {
        // Header
        Row(
            Modifier
                .fillMaxWidth()
                .background(LC.Surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Terminal, null, tint = LC.Green, modifier = Modifier.size(20.dp))
                Text("Console", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = LC.White)
                if (logs.isNotEmpty()) {
                    Text(
                        "${logs.size}",
                        fontSize = 10.sp,
                        color = LC.Green,
                        modifier = Modifier
                            .background(LC.Green.copy(0.12f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                // Toggle auto-scroll
                IconButton(
                    onClick = { autoScroll = !autoScroll },
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (autoScroll) LC.Green.copy(0.15f) else LC.CardAlt,
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        Icons.Outlined.KeyboardDoubleArrowDown,
                        null,
                        tint = if (autoScroll) LC.Green else LC.Muted,
                        modifier = Modifier.size(15.dp)
                    )
                }
                // Copiar tudo
                if (logs.isNotEmpty()) {
                    IconButton(
                        onClick = { clipboard.setText(AnnotatedString(logs.joinToString("\n"))) },
                        modifier = Modifier.size(32.dp).background(LC.CardAlt, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Outlined.ContentCopy, null, tint = LC.Muted, modifier = Modifier.size(15.dp))
                    }
                    // Limpar
                    IconButton(
                        onClick = { vm.clearLogs() },
                        modifier = Modifier.size(32.dp).background(LC.CardAlt, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Outlined.DeleteSweep, null, tint = LC.Error.copy(0.7f), modifier = Modifier.size(15.dp))
                    }
                }
            }
        }

        // Legenda de cores
        Row(
            Modifier
                .fillMaxWidth()
                .background(LC.Card)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendDot(LC.Error, "ERRO")
            LegendDot(LC.Warning, "AVISO")
            LegendDot(LC.Green, "OK")
            LegendDot(LC.Info, "Node")
            LegendDot(LC.Muted, "Info")
        }

        if (logs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Outlined.Terminal, null, tint = LC.Muted.copy(0.4f), modifier = Modifier.size(52.dp))
                    Text("Console vazio", color = LC.Muted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Os logs aparecerão aqui em tempo real", color = LC.Muted.copy(0.6f), fontSize = 11.sp)
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF080C08)),
                contentPadding = PaddingValues(4.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                itemsIndexed(logs, key = { i, _ -> i }) { _, line ->
                    val color: Color = when {
                        line.contains("[ERR/", true) || line.contains("error", true) ||
                        line.contains("falhou", true) || line.contains("failed", true) ||
                        line.contains("exception", true) || line.contains("crash", true) -> LC.Error

                        line.contains("warn", true) || line.contains("aviso", true) -> LC.Warning

                        line.contains("[Node]", true) || line.contains("node.stderr", true) -> LC.Info

                        line.contains("ok", true) || line.contains("sucesso", true) ||
                        line.contains("iniciado", true) || line.contains("extraído", true) ||
                        line.contains("encontrado", true) -> LC.Green

                        line.contains("NodeBridge", true) -> LC.Teal

                        else -> LC.Muted.copy(0.8f)
                    }

                    Text(
                        line,
                        color = color,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {},
                                onLongClick = { clipboard.setText(AnnotatedString(line)) }
                            )
                            .padding(horizontal = 8.dp, vertical = 1.dp)
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(7.dp).background(color, RoundedCornerShape(2.dp)))
        Text(label, fontSize = 9.sp, color = LC.Muted)
    }
}
