package com.limaxbot.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

object LC {
    val Green = Color(0xFF25D366)
    val GreenDark = Color(0xFF128C7E)
    val GreenDeep = Color(0xFF075E54)
    val Teal = Color(0xFF34B7F1)

    val Bg = Color(0xFF0A0F0A)
    val BgDark = Color(0xFF0F1410)
    val Surface = Color(0xFF111D13)
    val SurfaceVar = Color(0xFF1A2B1C)
    val Card = Color(0xFF162018)
    val CardAlt = Color(0xFF1C2A1E)
    val Border = Color(0xFF2A3D2C)

    val White = Color(0xFFE8EDE9)
    val Muted = Color(0xFF7A9A7D)

    val Success = Color(0xFF25D366)
    val Warning = Color(0xFFFFA726)
    val Error = Color(0xFFEF5350)
    val Info = Color(0xFF29B6F6)
    val Accent = Color(0xFF4CAF7D)
}

val LimaxColorScheme = darkColorScheme(
    primary = LC.Green,
    onPrimary = Color(0xFF003319),
    background = LC.Bg,
    onBackground = LC.White,
    surface = LC.Surface,
    onSurface = LC.White,
    surfaceVariant = LC.SurfaceVar,
    onSurfaceVariant = LC.Muted,
    outline = LC.Border,
    error = LC.Error
)
