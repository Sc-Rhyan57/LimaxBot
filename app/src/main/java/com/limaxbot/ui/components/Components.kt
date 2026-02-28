package com.limaxbot.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.limaxbot.ui.theme.LC

@Composable
fun LCard(title: String, icon: ImageVector, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth().border(1.dp, LC.Border, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = LC.Card),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(32.dp).background(LC.Green.copy(0.15f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = LC.Green, modifier = Modifier.size(16.dp))
                }
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = LC.White)
            }
            HorizontalDivider(color = LC.Border)
            content()
        }
    }
}

@Composable
fun LField(label: String, value: String, onValueChange: (String) -> Unit, placeholder: String = "", enabled: Boolean = true, leadingIcon: ImageVector? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 11.sp, color = LC.Muted, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = value, onValueChange = onValueChange, enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = LC.Muted.copy(0.6f), fontSize = 13.sp) },
            leadingIcon = leadingIcon?.let { { Icon(it, null, tint = LC.Muted, modifier = Modifier.size(18.dp)) } },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LC.Green, unfocusedBorderColor = LC.Border,
                focusedTextColor = LC.White, unfocusedTextColor = LC.White,
                disabledBorderColor = LC.Border.copy(0.5f), disabledTextColor = LC.Muted,
                cursorColor = LC.Green, focusedContainerColor = LC.CardAlt,
                unfocusedContainerColor = LC.CardAlt, disabledContainerColor = LC.CardAlt.copy(0.5f)
            ),
            shape = RoundedCornerShape(10.dp), singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
        )
    }
}

@Composable
fun LButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null, color: Color = LC.Green) {
    Button(
        onClick = onClick, modifier = modifier.fillMaxWidth().height(50.dp), enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = color, disabledContainerColor = color.copy(0.3f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        icon?.let { Icon(it, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
    }
}

@Composable
fun StatusBadge(text: String, color: Color) {
    Row(
        Modifier.background(color.copy(0.12f), RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(Modifier.size(6.dp).background(color, CircleShape))
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PulsingDot(color: Color = LC.Green) {
    val inf = rememberInfiniteTransition(label = "p")
    val alpha by inf.animateFloat(0.5f, 1f, infiniteRepeatable(tween(800, easing = EaseInOut), RepeatMode.Reverse), label = "a")
    Box(Modifier.size(9.dp).clip(CircleShape).background(color.copy(alpha)))
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 12.sp, color = LC.Muted)
        Text(value, fontSize = 12.sp, color = LC.White, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(32.dp)) {
            Box(Modifier.size(72.dp).background(LC.Green.copy(0.08f), CircleShape).border(1.dp, LC.Green.copy(0.2f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = LC.Green.copy(0.5f), modifier = Modifier.size(36.dp))
            }
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = LC.White)
            Text(subtitle, fontSize = 13.sp, color = LC.Muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
