package com.limaxbot

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import androidx.core.app.ActivityCompat
import com.limaxbot.ui.screens.*
import com.limaxbot.ui.theme.LC
import com.limaxbot.ui.theme.LimaxColorScheme
import com.limaxbot.viewmodel.BotViewModel

class MainActivity : ComponentActivity() {

    private val vm: BotViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPerms()

        val crashTrace = getSharedPreferences(LimaxApplication.PREF_CRASH, Context.MODE_PRIVATE)
            .getString(LimaxApplication.KEY_CRASH, null)

        if (crashTrace != null) {
            getSharedPreferences(LimaxApplication.PREF_CRASH, Context.MODE_PRIVATE)
                .edit().remove(LimaxApplication.KEY_CRASH).apply()
            setContent { LimaxTheme { CrashScreen(crashTrace) } }
            return
        }

        setContent { LimaxTheme { LimaxApp(vm) } }
    }

    private fun requestPerms() {
        val perms = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
                add(Manifest.permission.READ_MEDIA_IMAGES)
                add(Manifest.permission.READ_MEDIA_VIDEO)
                add(Manifest.permission.READ_MEDIA_AUDIO)
            }
        }
        val needed = perms.filter { ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (needed.isNotEmpty()) ActivityCompat.requestPermissions(this, needed.toTypedArray(), 100)
    }
}

@Composable
fun LimaxTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LimaxColorScheme, content = content)
}

@Composable
fun LimaxApp(vm: BotViewModel) {
    val state by vm.state.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    var footerClicks by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        NavItem("Home", Icons.Outlined.Home),
        NavItem("Mídias", Icons.Outlined.PermMedia),
        NavItem("Apagadas", Icons.Outlined.DeleteSweep),
        NavItem("Contatos", Icons.Outlined.People),
        NavItem("Config", Icons.Outlined.Settings),
        NavItem("Console", Icons.Outlined.Terminal),
    )

    Scaffold(
        containerColor = LC.Bg,
        snackbarHost = {
            state.error?.let { err ->
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.BottomCenter) {
                    Card(colors = CardDefaults.cardColors(containerColor = LC.Error), shape = RoundedCornerShape(12.dp)) {
                        Row(Modifier.padding(12.dp, 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.ErrorOutline, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text(err, color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }
        },
        bottomBar = {
            Column {
                NavigationBar(containerColor = LC.Surface, tonalElevation = 0.dp) {
                    tabs.forEachIndexed { i, item ->
                        // Badge de log count na aba Console
                        val badge = if (i == 5 && state.logLines.isNotEmpty()) state.logLines.size else 0
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = {
                                BadgedBox(badge = {
                                    if (badge > 0 && tab != 5) {
                                        Badge(containerColor = LC.Error) {
                                            Text(if (badge > 99) "99+" else "$badge", fontSize = 7.sp)
                                        }
                                    }
                                }) {
                                    Icon(item.icon, item.label, modifier = Modifier.size(22.dp))
                                }
                            },
                            label = { Text(item.label, fontSize = 9.sp, fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = LC.Green, selectedTextColor = LC.Green,
                                unselectedIconColor = LC.Muted, unselectedTextColor = LC.Muted,
                                indicatorColor = LC.Green.copy(0.14f)
                            )
                        )
                    }
                }
                Footer(footerClicks) { footerClicks++ }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            AnimatedContent(targetState = tab) { t ->
                when (t) {
                    0 -> HomeScreen(vm)
                    1 -> MediaScreen(vm)
                    2 -> DeletedScreen(vm)
                    3 -> ContactsScreen(vm)
                    4 -> SettingsScreen(vm)
                    5 -> ConsoleScreen(vm)
                }
            }
        }
    }
}

@Composable
fun Footer(clicks: Int, onClick: () -> Unit) {
    val ctx = LocalContext.current
    val t = rememberInfiniteTransition(label = "r")
    val hue by t.animateFloat(80f, 160f, infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Reverse), label = "h")
    val c = Color.hsv(hue, 0.65f, 0.85f)

    Column(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.clickable { onClick() }) {
            Icon(Icons.Outlined.AutoAwesome, null, tint = c, modifier = Modifier.size(11.dp))
            Text("By Rhyan57", fontSize = 11.sp, color = c, fontWeight = FontWeight.Bold)
            Icon(Icons.Outlined.AutoAwesome, null, tint = c, modifier = Modifier.size(11.dp))
        }
        Text("Inspirado em LIMAXBOT", fontSize = 9.sp, color = LC.Muted.copy(0.4f), fontStyle = FontStyle.Italic)
        Spacer(Modifier.height(2.dp))
        OutlinedButton(
            onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Sc-Rhyan57/McpeBot"))) },
            modifier = Modifier.fillMaxWidth().height(36.dp).padding(horizontal = 16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = LC.Muted),
            border = BorderStroke(1.dp, LC.Border),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Outlined.Code, null, modifier = Modifier.size(14.dp), tint = LC.Muted)
            Spacer(Modifier.width(6.dp))
            Text("Sc-Rhyan57/McpeBot", fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
fun CrashScreen(trace: String) {
    val clipboard = LocalClipboardManager.current
    Column(Modifier.fillMaxSize().background(LC.Bg).padding(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.BugReport, null, tint = LC.Error, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(10.dp))
                Text("App Crashou", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold), color = LC.White)
            }
            TextButton(onClick = { android.os.Process.killProcess(android.os.Process.myPid()) }) {
                Text("Fechar", color = LC.Error, fontWeight = FontWeight.Bold)
            }
        }
        Text("Um erro inesperado ocorreu.", color = LC.Muted, modifier = Modifier.padding(bottom = 12.dp))
        Button(onClick = { clipboard.setText(AnnotatedString(trace)) }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LC.Green), shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Copiar Log do Crash", fontWeight = FontWeight.Bold)
        }
        Card(modifier = Modifier.fillMaxSize(), colors = CardDefaults.cardColors(containerColor = LC.Error.copy(0.07f)), shape = RoundedCornerShape(12.dp)) {
            LazyColumn(Modifier.fillMaxSize()) {
                item { Text(trace, Modifier.padding(12.dp), fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = LC.Error.copy(0.9f), lineHeight = 16.sp) }
            }
        }
    }
}

data class NavItem(val label: String, val icon: ImageVector)
