package com.limaxbot.ui.screens

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.limaxbot.model.BotStatus
import com.limaxbot.model.ContactInfo
import com.limaxbot.ui.components.*
import com.limaxbot.ui.theme.LC
import com.limaxbot.viewmodel.BotViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ContactsScreen(vm: BotViewModel) {
    val state by vm.state.collectAsState()
    var number by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(state.contactResult) { if (state.contactResult != null) loading = false }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(LC.Bg, LC.BgDark))),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("Pesquisa de Contatos", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = LC.White)
                Text("Busque info de qualquer número WhatsApp", fontSize = 12.sp, color = LC.Muted)
            }
        }

        item {
            LCard("Buscar Número", Icons.Outlined.Search) {
                LField("Número com DDI (somente números)", number, { number = it }, "5541999998888", state.status == BotStatus.CONNECTED, Icons.Outlined.Phone)

                if (state.status != BotStatus.CONNECTED) {
                    Row(Modifier.fillMaxWidth().background(LC.Warning.copy(0.08f), RoundedCornerShape(8.dp)).border(1.dp, LC.Warning.copy(0.25f), RoundedCornerShape(8.dp)).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Outlined.WifiOff, null, tint = LC.Warning, modifier = Modifier.size(14.dp))
                        Text("Bot precisa estar conectado", color = LC.Warning, fontSize = 12.sp)
                    }
                }

                LButton(
                    text = if (loading) "Buscando..." else "Buscar Informações",
                    onClick = { if (number.isNotBlank()) { loading = true; vm.fetchContact(number.trim()) } },
                    enabled = number.isNotBlank() && state.status == BotStatus.CONNECTED && !loading,
                    icon = if (loading) Icons.Outlined.Sync else Icons.Outlined.PersonSearch
                )
            }
        }

        state.contactResult?.let { info ->
            item { ContactCard(info) }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun ContactCard(info: ContactInfo) {
    val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, LC.Green.copy(0.3f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = LC.Card),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(70.dp).clip(CircleShape).background(LC.CardAlt).border(2.dp, LC.Green.copy(0.4f), CircleShape), contentAlignment = Alignment.Center) {
                    if (info.profilePic != null) {
                        AsyncImage(model = info.profilePic, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Outlined.Person, null, tint = LC.Muted, modifier = Modifier.size(34.dp))
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("+${info.number}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = LC.White)
                    Text(info.jid.take(28), fontSize = 11.sp, color = LC.Muted, fontFamily = FontFamily.Monospace)
                    StatusBadge("WhatsApp", LC.Green)
                }
            }
            HorizontalDivider(color = LC.Border)
            if (info.status != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Recado", fontSize = 11.sp, color = LC.Muted, fontWeight = FontWeight.SemiBold)
                    Text(info.status, fontSize = 13.sp, color = LC.White, modifier = Modifier.fillMaxWidth().background(LC.CardAlt, RoundedCornerShape(8.dp)).padding(10.dp))
                }
            } else {
                Text("Recado não disponível", fontSize = 12.sp, color = LC.Muted)
            }
            Text("Consultado: ${fmt.format(Date(info.fetchedAt))}", color = LC.Muted, fontSize = 11.sp)
            if (info.profilePic != null) {
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, LC.Green.copy(0.5f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Outlined.Download, null, tint = LC.Green, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Ver/Baixar Foto de Perfil", color = LC.Green, fontSize = 13.sp)
                }
            }
        }
    }
}
