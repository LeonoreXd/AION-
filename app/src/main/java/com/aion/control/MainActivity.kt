package com.aion.control

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.aion.control.accessibility.AionAccessibilityService
import com.aion.control.apps.AppController
import com.aion.control.apps.LaunchableApp
import com.aion.control.device.DeviceController
import com.aion.control.device.DeviceMonitor
import com.aion.control.device.DeviceSnapshot
import com.aion.control.commands.AionCommandEngine
import com.aion.control.commands.AionAction
import com.aion.control.commands.AionPlan

private val Ink = Color(0xFF090E18)
private val Panel = Color(0xFF121C2B)
private val Blue = Color(0xFF4C9DFF)
private val Cyan = Color(0xFF69D7E8)
private val Muted = Color(0xFF91A1B8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AionTheme { AionDashboard() } }
    }
}

@Composable
private fun AionTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(
        primary = Blue, secondary = Cyan, background = Ink, surface = Panel,
        onBackground = Color.White, onSurface = Color.White
    ), content = content)
}

@Composable
private fun AionDashboard() {
    val context = LocalContext.current
    var selected by remember { mutableStateOf("Home") }
    var command by remember { mutableStateOf("") }
    var commandPlan by remember { mutableStateOf<AionPlan?>(null) }
    var commandStatus by remember { mutableStateOf("Commands are interpreted on this device") }
    var snapshot by remember { mutableStateOf<DeviceSnapshot?>(null) }
    var apps by remember { mutableStateOf<List<LaunchableApp>>(emptyList()) }
    var appSearch by remember { mutableStateOf("") }
    var appStatus by remember { mutableStateOf("Loading apps…") }
    var volume by remember { mutableFloatStateOf(50f) }
    var status by remember { mutableStateOf("Tap refresh to read device metrics") }
    var uiQuery by remember { mutableStateOf("") }
    var uiText by remember { mutableStateOf("") }
    var uiStatus by remember { mutableStateOf("Enable AION Accessibility in Android settings") }
    val monitor = remember { DeviceMonitor(context) }
    val appController = remember { AppController(context) }
    val deviceController = remember { DeviceController(context) }
    val commandEngine = remember { AionCommandEngine() }
    val tabs = listOf("Home", "AI", "Apps", "Monitor", "Control")

    fun refreshMetrics() {
        runCatching { monitor.readSnapshot() }
            .onSuccess { snapshot = it; status = "Updated just now" }
            .onFailure { status = "Could not read metrics: ${it.message}" }
    }

    LaunchedEffect(selected) {
        if (selected == "Apps" && apps.isEmpty()) {
            runCatching { appController.getLaunchableApps() }
                .onSuccess { apps = it; appStatus = "${it.size} launchable apps found" }
                .onFailure { appStatus = "Could not load apps: ${it.message}" }
        }
    }

    Scaffold(
        containerColor = Ink,
        bottomBar = {
            NavigationBar(containerColor = Panel) {
                val icons = listOf(Icons.Default.Home, Icons.Default.Chat, Icons.Default.Apps,
                    Icons.Default.MonitorHeart, Icons.Default.TouchApp)
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selected == tab, onClick = { selected = tab },
                        icon = { Icon(icons[index], contentDescription = tab) },
                        label = { Text(tab, fontSize = 10.sp) }
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("AION", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        Text("DEVICE INTELLIGENCE", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
                    }
                    StatusPill("LOCAL", "Ready")
                }
            }
            if (selected == "Home" || selected == "AI") {
                item {
                    Column(Modifier.fillMaxWidth().background(
                        androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF172B45), Color(0xFF10202E))),
                        RoundedCornerShape(24.dp)).padding(20.dp)) {
                        Text("CONTROL CENTER", color = Cyan, fontSize = 10.sp, letterSpacing = 2.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Your device.\nOne command away.", color = Color.White,
                            fontSize = 27.sp, lineHeight = 33.sp, fontWeight = FontWeight.SemiBold)
                        Text("A calm interface for powerful device management.",
                            color = Muted, fontSize = 13.sp)
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusPill("ROOT", "Not configured")
                            StatusPill("AI BRIDGE", "Not connected")
                        }
                    }
                }
            }
            if (selected == "Home" || selected == "Monitor") {
                item {
                    SectionTitle("DEVICE OVERVIEW", status)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricCard("RAM", snapshot?.let { "${it.ramPercent}%" } ?: "--",
                            snapshot?.let { "${it.formatBytes(it.ramUsedBytes)} used" } ?: "Not loaded", Modifier.weight(1f))
                        MetricCard("STORAGE", snapshot?.let { "${it.storagePercent}%" } ?: "--",
                            snapshot?.let { "${it.formatBytes(it.storageUsedBytes)} used" } ?: "Not loaded", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricCard("BATTERY", snapshot?.batteryPercent?.let { "$it%" } ?: "--",
                            if (snapshot?.isCharging == true) "Charging" else "Battery level", Modifier.weight(1f))
                        MetricCard("ANDROID", snapshot?.androidVersion ?: "--",
                            snapshot?.let { "${it.manufacturer} ${it.model}" } ?: "Device version", Modifier.weight(1f))
                    }
                    OutlinedButton(onClick = { refreshMetrics() }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Refresh device metrics")
                    }
                }
            }
            if (selected == "Apps") {
                item {
                    SectionTitle("APPLICATION CONTROL", appStatus)
                    OutlinedTextField(value = appSearch, onValueChange = { appSearch = it },
                        modifier = Modifier.fillMaxWidth(), placeholder = { Text("Search installed apps…") },
                        singleLine = true)
                    OutlinedButton(onClick = {
                        runCatching { appController.getLaunchableApps() }
                            .onSuccess { apps = it; appStatus = "${it.size} launchable apps found" }
                            .onFailure { appStatus = "Could not load apps: ${it.message}" }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Refresh app list") }
                }
                items(apps.filter { it.label.contains(appSearch, ignoreCase = true) }) { app ->
                    Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp)).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(app.label, color = Color.White, fontWeight = FontWeight.Medium)
                            Text(app.packageName, color = Muted, fontSize = 10.sp)
                        }
                        TextButton(onClick = {
                            appStatus = if (runCatching { appController.launch(app.packageName) }.getOrDefault(false))
                                "Opened ${app.label}" else "Unable to launch ${app.label}"
                        }) { Text("Open") }
                    }
                }
            }
            if (selected == "Monitor") {
                item {
                    SectionTitle("DEVICE CONTROLS", "Safe Android controls")
                    Button(onClick = { deviceController.openSystemSettings() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Open Android Settings")
                    }
                    OutlinedButton(onClick = { deviceController.openDisplaySettings() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Display settings")
                    }
                    OutlinedButton(onClick = { deviceController.openSoundSettings() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Sound settings")
                    }
                    Text("Media volume · ${volume.toInt()}%", color = Color.White)
                    Slider(value = volume, onValueChange = { volume = it },
                        onValueChangeFinished = { deviceController.setMediaVolume(volume.toInt()) })
                }
            }
            if (selected == "AI") {
                item {
                    SectionTitle("AI COMMAND CENTER", "Local allowlist · confirmation required")
                    OutlinedTextField(value = command, onValueChange = { command = it; commandPlan = null },
                        modifier = Modifier.fillMaxWidth(), placeholder = { Text("Contoh: buka pengaturan / buka Chrome") }, minLines = 2)
                    Button(onClick = {
                        if (apps.isEmpty()) apps = runCatching { appController.getLaunchableApps() }.getOrDefault(emptyList())
                        commandPlan = commandEngine.plan(command, apps)
                        commandStatus = when (val plan = commandPlan) {
                            is AionPlan.Ready -> "Ready for your confirmation"
                            is AionPlan.Unknown -> plan.reason
                            null -> "No command prepared"
                        }
                    }, enabled = command.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Analyze command") }
                    when (val plan = commandPlan) {
                        is AionPlan.Ready -> {
                            Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(16.dp)).padding(14.dp)) {
                                Text(plan.action.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text(plan.action.detail, color = Muted, fontSize = 11.sp)
                                Spacer(Modifier.height(10.dp))
                                Button(onClick = {
                                    val ok = runCatching {
                                        when (val action = plan.action) {
                                            AionAction.OpenSettings -> { deviceController.openSystemSettings(); true }
                                            AionAction.OpenDisplay -> { deviceController.openDisplaySettings(); true }
                                            AionAction.OpenSound -> { deviceController.openSoundSettings(); true }
                                            is AionAction.LaunchApp -> appController.launch(action.app.packageName)
                                        }
                                    }.getOrDefault(false)
                                    commandStatus = if (ok) "Action launched" else "Could not launch action"
                                    commandPlan = null
                                }, modifier = Modifier.fillMaxWidth()) { Text("Confirm and execute") }
                            }
                        }
                        is AionPlan.Unknown -> Text(plan.reason, color = Cyan, fontSize = 12.sp)
                        null -> Unit
                    }
                    Text(commandStatus, color = Muted, fontSize = 11.sp)
                    Text("AION only executes predefined actions. Unsupported commands are rejected; no arbitrary terminal commands are run.",
                        color = Muted, fontSize = 11.sp)
                }
            }
            if (selected == "Control") {
                item {
                    SectionTitle("UI AUTOMATION", uiStatus)
                    OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                        modifier = Modifier.fillMaxWidth()) { Text("Enable AION Accessibility") }
                    Text("Navigation", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { uiStatus = if (AionAccessibilityService.current?.goBack() == true) "Back action sent" else "Service unavailable" }, modifier = Modifier.weight(1f)) { Text("Back") }
                        OutlinedButton(onClick = { uiStatus = if (AionAccessibilityService.current?.goHome() == true) "Home action sent" else "Service unavailable" }, modifier = Modifier.weight(1f)) { Text("Home") }
                        OutlinedButton(onClick = { uiStatus = if (AionAccessibilityService.current?.openRecents() == true) "Recents action sent" else "Service unavailable" }, modifier = Modifier.weight(1f)) { Text("Recents") }
                    }
                    Text("Find and click visible text", color = Color.White, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(value = uiQuery, onValueChange = { uiQuery = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Visible button or label") }, singleLine = true)
                    Button(onClick = { uiStatus = if (AionAccessibilityService.current?.clickText(uiQuery) == true) "Click action sent" else "No clickable match or service unavailable" }, enabled = uiQuery.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Click matching text") }
                    Text("Type into the currently focused field", color = Color.White, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(value = uiText, onValueChange = { uiText = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Text to enter") })
                    Button(onClick = { uiStatus = if (AionAccessibilityService.current?.typeIntoFocusedField(uiText) == true) "Text entered" else "No focused editable field or service unavailable" }, enabled = uiText.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("Type text") }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { uiStatus = if (AionAccessibilityService.current?.scrollUp() == true) "Scrolled up" else "No scrollable view or service unavailable" }, modifier = Modifier.weight(1f)) { Text("Scroll up") }
                        OutlinedButton(onClick = { uiStatus = if (AionAccessibilityService.current?.scrollDown() == true) "Scrolled down" else "No scrollable view or service unavailable" }, modifier = Modifier.weight(1f)) { Text("Scroll down") }
                    }
                    Text("Actions are only triggered by your taps here. AION does not record screen contents.", color = Muted, fontSize = 11.sp)
                }
            }
            item { Text("AION CONTROL • FOUNDATION 0.5.0", color = Muted, fontSize = 10.sp) }
        }
    }
}

@Composable
private fun StatusPill(label: String, value: String) {
    Column(Modifier.background(Color(0x332B496B), RoundedCornerShape(12.dp)).padding(10.dp)) {
        Text(label, color = Cyan, fontSize = 9.sp, letterSpacing = 1.sp)
        Text(value, color = Color.White, fontSize = 11.sp)
    }
}
@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(Modifier.padding(bottom = 10.dp)) {
        Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Text(subtitle, color = Muted, fontSize = 11.sp)
    }
}
@Composable
private fun MetricCard(name: String, value: String, caption: String, modifier: Modifier) {
    Column(modifier.background(Panel, RoundedCornerShape(18.dp)).padding(15.dp)) {
        Text(name, color = Muted, fontSize = 10.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        Text(value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Text(caption, color = Muted, fontSize = 10.sp)
    }
}
@Composable
private fun ModuleRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF1A2B41)) {
            Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Blue) }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Muted, fontSize = 11.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = Muted)
    }
}
