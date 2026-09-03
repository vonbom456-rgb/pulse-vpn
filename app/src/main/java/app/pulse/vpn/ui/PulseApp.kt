package app.pulse.vpn.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pulse.vpn.PulseUiState
import app.pulse.vpn.PulseViewModel
import app.pulse.vpn.Screen
import app.pulse.vpn.core.SettingsManager
import app.pulse.vpn.data.ProfileRepository
import app.pulse.vpn.data.VpnProfile
import app.pulse.vpn.data.VpnServer
import io.nekohasekai.sfa.constant.Status
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date
import kotlin.math.max

@Composable
fun PulseApp(
    viewModel: PulseViewModel,
    requestConnect: () -> Unit,
    scanQr: () -> Unit,
    openVpnSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    var showImport by remember { mutableStateOf(false) }
    if (showImport) ImportDialog(
        loading = state.importing,
        onDismiss = { if (!state.importing) showImport = false },
        onImport = { showImport = false; viewModel.import(it) },
        scanQr = { showImport = false; scanQr() },
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (state.screen in setOf(Screen.HOME, Screen.ROUTES, Screen.STATS, Screen.SETTINGS)) {
                PulseNavigation(state.screen, viewModel::navigate)
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            AnimatedContent(targetState = state.screen, label = "screen") { screen ->
                when (screen) {
                    Screen.HOME -> HomeScreen(state, requestConnect, viewModel::stopVpn, { viewModel.navigate(Screen.ROUTES) }, { viewModel.navigate(Screen.PROFILES) }, { showImport = true })
                    Screen.ROUTES -> RoutesScreen(state, viewModel::selectServer, viewModel::testServers, { showImport = true })
                    Screen.STATS -> StatsScreen(state)
                    Screen.SETTINGS -> SettingsScreen(state, viewModel, openVpnSettings)
                    Screen.PROFILES -> ProfilesScreen(state, { viewModel.navigate(Screen.HOME) }, { showImport = true }, viewModel::selectProfile, viewModel::updateProfile, viewModel::deleteProfile)
                    Screen.APPS -> AppsScreen(state, { viewModel.navigate(Screen.SETTINGS) }, viewModel::setPerAppMode, viewModel::toggleApp)
                }
            }
            AnimatedVisibility(
                visible = state.message != null,
                enter = fadeIn() + scaleIn(initialScale = .96f),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp, start = 20.dp, end = 20.dp),
            ) {
                state.message?.let { InlineBanner(it, viewModel::clearMessage) }
            }
        }
    }
}

@Composable
private fun PulseNavigation(current: Screen, navigate: (Screen) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .97f),
        tonalElevation = 0.dp,
        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = .06f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
    ) {
        listOf(
            Triple(Screen.HOME, Icons.Outlined.Home, "Главная"),
            Triple(Screen.ROUTES, Icons.Outlined.Route, "Маршруты"),
            Triple(Screen.STATS, Icons.Outlined.AutoGraph, "Скорость"),
            Triple(Screen.SETTINGS, Icons.Outlined.Settings, "Настройки"),
        ).forEach { (screen, icon, label) ->
            NavigationBarItem(
                selected = current == screen,
                onClick = { navigate(screen) },
                icon = { Icon(icon, null) },
                label = { Text(label, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PulseColors.Cyan,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = PulseColors.Cyan.copy(alpha = .12f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .45f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .45f),
                ),
            )
        }
    }
}

@Composable
private fun HomeScreen(
    state: PulseUiState,
    connect: () -> Unit,
    disconnect: () -> Unit,
    routes: () -> Unit,
    profiles: () -> Unit,
    addProfile: () -> Unit,
) {
    val selected = state.servers.firstOrNull(VpnServer::selected) ?: state.servers.firstOrNull()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 18.dp, start = 22.dp, end = 22.dp, bottom = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PulseMark(42.dp)
            Spacer(Modifier.width(12.dp))
            Column { Text("PULSE", fontWeight = FontWeight.Bold, letterSpacing = 2.sp); Text("VPN", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .46f), fontSize = 12.sp) }
            Spacer(Modifier.weight(1f))
            StatusPill(state.vpnStatus)
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = profiles) { Icon(Icons.Outlined.Devices, "Профили") }
        }

        Spacer(Modifier.height(26.dp))
        PulseConnectButton(state.vpnStatus, if (state.vpnStatus == Status.Started || state.vpnStatus == Status.Starting) disconnect else connect)
        Spacer(Modifier.height(20.dp))

        if (state.selectedProfile == null) {
            EmptyCard(
                icon = Icons.Outlined.Add,
                title = "Добавьте подписку",
                text = "Ссылка, QR, VLESS, Clash или sing-box JSON",
                action = "Добавить",
                onClick = addProfile,
            )
        } else {
            PremiumCard(onClick = routes) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Outlined.Language)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(selected?.tag ?: "Выберите маршрут", style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            listOfNotNull(selected?.type?.uppercase(), selected?.delayMs?.let { "$it мс" }).joinToString("  ·  ").ifBlank { state.selectedProfile.name },
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .5f), fontSize = 13.sp,
                        )
                    }
                    Icon(Icons.Outlined.SwapVert, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .45f))
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("↓", formatSpeed(state.traffic.downloadPerSecond), "СКАЧИВАНИЕ", Modifier.weight(1f), PulseColors.Cyan)
                MetricCard("↑", formatSpeed(state.traffic.uploadPerSecond), "ОТДАЧА", Modifier.weight(1f), PulseColors.Violet)
            }
        }
    }
}

@Composable
private fun PulseConnectButton(status: Status, onClick: () -> Unit) {
    val active = status == Status.Started
    val moving = status == Status.Starting || status == Status.Stopping
    val infinite = rememberInfiniteTransition(label = "pulse")
    val phase by infinite.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(if (moving) 850 else 1800, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "phase",
    )
    val haptic = LocalHapticFeedback.current
    Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(290.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            repeat(3) { index ->
                val progress = if (active || moving) (phase + index / 3f) % 1f else index / 3f
                drawCircle(
                    color = (if (active) PulseColors.Success else PulseColors.Violet).copy(alpha = if (active || moving) (1f - progress) * .24f else .08f),
                    radius = size.minDimension * (.24f + .25f * progress), center = center,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }
        Box(
            Modifier.size(172.dp).clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = if (active) listOf(Color(0xFF00E6A0), Color(0xFF007F70)) else listOf(Color(0xFF8172F8), Color(0xFF314B9A)),
                    ),
                )
                .border(1.dp, Color.White.copy(alpha = .3f), CircleShape)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (moving) CircularProgressIndicator(Modifier.size(32.dp), color = Color.White, strokeWidth = 2.dp)
                else Icon(Icons.Outlined.PowerSettingsNew, null, tint = Color.White, modifier = Modifier.size(38.dp))
                Spacer(Modifier.height(9.dp))
                Text(
                    when (status) { Status.Started -> "ПОДКЛЮЧЕНО"; Status.Starting -> "ПОДКЛЮЧЕНИЕ"; Status.Stopping -> "ОТКЛЮЧЕНИЕ"; else -> "ПОДКЛЮЧИТЬ" },
                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.2.sp,
                )
            }
        }
    }
}

@Composable
private fun RoutesScreen(state: PulseUiState, select: (VpnServer) -> Unit, test: () -> Unit, add: () -> Unit) {
    ScreenColumn {
        ScreenHeader("Маршруты", trailing = {
            IconButton(onClick = test, enabled = state.servers.isNotEmpty()) { Icon(Icons.Outlined.Speed, "Проверить задержку") }
            IconButton(onClick = add) { Icon(Icons.Outlined.Add, "Добавить") }
        })
        Text("Выберите выходной узел. Список читается прямо из активной подписки.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .48f), fontSize = 14.sp)
        Spacer(Modifier.height(22.dp))
        when {
            state.selectedProfile == null -> EmptyCard(Icons.Outlined.Route, "Маршрутов пока нет", "Добавьте подписку — экран не будет бесконечно загружаться.", "Добавить подписку", add)
            state.servers.isEmpty() -> EmptyCard(Icons.Outlined.Info, "В профиле нет прокси", "В конфигурации не найдены поддерживаемые outbound-серверы.", "Добавить другой", add)
            else -> state.servers.forEach { server ->
                ServerRow(server, { select(server) })
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun ServerRow(server: VpnServer, onClick: () -> Unit) {
    PremiumCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(
                    Brush.linearGradient(listOf(PulseColors.Violet.copy(.35f), PulseColors.Cyan.copy(.22f))),
                ), contentAlignment = Alignment.Center,
            ) { Text(server.tag.take(2).uppercase(), fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(server.tag, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(listOfNotNull(server.type.uppercase(), server.address).joinToString(" · "), color = MaterialTheme.colorScheme.onSurface.copy(.45f), fontSize = 12.sp, maxLines = 1)
            }
            if (server.delayMs != null) Text("${server.delayMs} мс", color = if (server.delayMs < 250) PulseColors.Success else Color(0xFFFFB86B), fontSize = 12.sp)
            if (server.selected) { Spacer(Modifier.width(10.dp)); Icon(Icons.Outlined.Check, null, tint = PulseColors.Cyan) }
        }
    }
}

@Composable
private fun ProfilesScreen(
    state: PulseUiState,
    back: () -> Unit,
    add: () -> Unit,
    select: (VpnProfile) -> Unit,
    update: (VpnProfile) -> Unit,
    delete: (VpnProfile) -> Unit,
) {
    ScreenColumn {
        ScreenHeader("VPN-профили", back, { IconButton(onClick = add) { Icon(Icons.Outlined.Add, null) } })
        if (state.profiles.isEmpty()) {
            EmptyCard(Icons.Outlined.Add, "Нет профилей", "Импортируйте ссылку вашего VPN-провайдера или конфигурацию.", "Добавить", add)
        } else state.profiles.forEach { profile ->
            PremiumCard(onClick = { select(profile) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PulseMark(46.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(profile.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                            if (state.selectedProfile?.id == profile.id) Text("  ACTIVE", color = PulseColors.Success, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        val expiry = profile.expireAt?.takeIf { it > 0 }?.let { DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it * 1000)) }
                        Text(expiry?.let { "До $it" } ?: if (profile.sourceUrl != null) "Удалённая подписка" else "Локальная конфигурация", color = MaterialTheme.colorScheme.onSurface.copy(.45f), fontSize = 12.sp)
                    }
                    if (profile.sourceUrl != null) IconButton(onClick = { update(profile) }) { Icon(Icons.Outlined.Refresh, "Обновить") }
                    IconButton(onClick = { delete(profile) }) { Icon(Icons.Outlined.DeleteOutline, "Удалить", tint = PulseColors.Danger) }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        if (state.importing) LinearLoading()
    }
}

@Composable
private fun StatsScreen(state: PulseUiState) {
    val samples = remember { mutableStateListOf<Long>() }
    LaunchedEffect(state.traffic.downloadPerSecond) {
        samples += state.traffic.downloadPerSecond
        if (samples.size > 40) samples.removeAt(0)
    }
    ScreenColumn {
        ScreenHeader("Скорость")
        Text(if (state.vpnStatus == Status.Started) "Данные текущей сессии" else "Подключитесь, чтобы увидеть живой трафик", color = MaterialTheme.colorScheme.onSurface.copy(.48f))
        Spacer(Modifier.height(24.dp))
        PremiumCard {
            Text("СКОРОСТЬ ЗАГРУЗКИ", color = MaterialTheme.colorScheme.onSurface.copy(.42f), fontSize = 11.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))
            Text(formatSpeed(state.traffic.downloadPerSecond), style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(24.dp))
            SpeedGraph(samples)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("↓", formatBytes(state.traffic.downloadTotal), "ПОЛУЧЕНО", Modifier.weight(1f), PulseColors.Cyan)
            MetricCard("↑", formatBytes(state.traffic.uploadTotal), "ОТПРАВЛЕНО", Modifier.weight(1f), PulseColors.Violet)
        }
    }
}

@Composable
private fun SpeedGraph(samples: List<Long>) {
    Canvas(Modifier.fillMaxWidth().height(170.dp)) {
        if (samples.size < 2) return@Canvas
        val peak = max(1L, samples.max()).toFloat()
        val step = size.width / (samples.size - 1)
        val points = samples.mapIndexed { index, value -> Offset(index * step, size.height - (value / peak) * size.height * .86f) }
        for (i in 0 until points.lastIndex) drawLine(
            brush = Brush.horizontalGradient(listOf(PulseColors.Violet, PulseColors.Cyan)),
            start = points[i], end = points[i + 1], strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun SettingsScreen(state: PulseUiState, viewModel: PulseViewModel, openVpnSettings: () -> Unit) {
    ScreenColumn {
        ScreenHeader("Настройки")
        SectionLabel("ПОДКЛЮЧЕНИЕ")
        SettingsCard {
            SettingSwitch(Icons.Outlined.Bolt, "Автоподключение", "Запуск VPN после перезагрузки", state.autoConnect, viewModel::setAutoConnect)
            DividerInset()
            SettingAction(Icons.Outlined.Lock, "Kill switch", "Always-on VPN и блокировка без VPN", openVpnSettings)
            DividerInset()
            SettingAction(Icons.Outlined.Apps, "Маршрутизация приложений", perAppLabel(state), { viewModel.navigate(Screen.APPS) })
        }
        SectionLabel("МАРШРУТИЗАЦИЯ")
        SettingsCard {
            ChoiceRow("Режим трафика", state.routingMode, listOf("rules" to "Правила", "global" to "Весь VPN", "direct" to "Напрямую"), viewModel::setRoutingMode)
            DividerInset()
            ChoiceRow("DNS", state.dnsMode, listOf("local" to "Из профиля", "cloudflare" to "Cloudflare", "google" to "Google"), viewModel::setDnsMode)
        }
        SectionLabel("ВИД")
        SettingsCard { SettingSwitch(Icons.Outlined.Tune, "Тёмная тема", "Фирменная тема Pulse", state.darkTheme, viewModel::setDarkTheme) }
        SectionLabel("О ПРИЛОЖЕНИИ")
        SettingsCard {
            SettingAction(Icons.Outlined.Info, "Pulse VPN 0.3", "Kotlin · Compose · sing-box", {})
        }
    }
}

@Composable
private fun AppsScreen(state: PulseUiState, back: () -> Unit, setMode: (Int) -> Unit, toggle: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = back) { Icon(Icons.Outlined.ArrowBack, null) }
            Text("Приложения", style = MaterialTheme.typography.headlineMedium)
        }
        Text("Выберите приложения для текущего режима. Изменения применятся при следующем подключении.", color = MaterialTheme.colorScheme.onSurface.copy(.48f), modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                SettingsManager.Keys.PER_APP_PROXY_DISABLED to "Все",
                SettingsManager.Keys.PER_APP_PROXY_INCLUDE to "Только",
                SettingsManager.Keys.PER_APP_PROXY_EXCLUDE to "Кроме",
            ).forEach { (mode, label) ->
                val active = state.perAppMode == mode
                Box(
                    Modifier.weight(1f).clip(CircleShape)
                        .background(if (active) PulseColors.Violet.copy(.25f) else MaterialTheme.colorScheme.surface)
                        .clickable { setMode(mode) }.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(label, color = if (active) PulseColors.Cyan else MaterialTheme.colorScheme.onSurface.copy(.5f), fontSize = 12.sp) }
            }
        }
        LazyColumn(contentPadding = PaddingValues(14.dp, 8.dp, 14.dp, 32.dp)) {
            items(state.apps, key = ProfileRepository.AppEntry::packageName) { app ->
                Row(
                    Modifier.fillMaxWidth().clickable { toggle(app.packageName) }.padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                        Text(app.label.take(1).uppercase(), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) { Text(app.label); Text(app.packageName, color = MaterialTheme.colorScheme.onSurface.copy(.38f), fontSize = 11.sp) }
                    androidx.compose.material3.Checkbox(checked = app.packageName in state.selectedApps, onCheckedChange = { toggle(app.packageName) })
                }
            }
        }
    }
}

@Composable
private fun ImportDialog(loading: Boolean, onDismiss: () -> Unit, onImport: (String) -> Unit, scanQr: () -> Unit) {
    var value by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Добавить подписку") },
        text = {
            Column {
                Text("Вставьте ссылку или полный конфиг. Данные хранятся только на устройстве.", color = MaterialTheme.colorScheme.onSurface.copy(.55f))
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value, { value = it }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 8, placeholder = { Text("https://… или vless://…") })
                Spacer(Modifier.height(10.dp))
                Row {
                    TextButton(onClick = { clipboard.getText()?.text?.let { value = it } }) { Icon(Icons.Outlined.ContentPaste, null); Spacer(Modifier.width(6.dp)); Text("Вставить") }
                    TextButton(onClick = scanQr) { Icon(Icons.Outlined.QrCodeScanner, null); Spacer(Modifier.width(6.dp)); Text("QR") }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onImport(value) }, enabled = value.isNotBlank() && !loading, colors = ButtonDefaults.buttonColors(containerColor = PulseColors.Violet)) {
                if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White) else Text("Импортировать")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun StatusPill(status: Status) {
    val (text, color) = when (status) {
        Status.Started -> "ONLINE" to PulseColors.Success
        Status.Starting, Status.Stopping -> "WAIT" to Color(0xFFFFC46B)
        Status.Stopped -> "OFFLINE" to MaterialTheme.colorScheme.onSurface.copy(.38f)
    }
    Row(Modifier.clip(CircleShape).background(color.copy(.10f)).border(1.dp, color.copy(.25f), CircleShape).padding(horizontal = 11.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color)); Spacer(Modifier.width(6.dp)); Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
private fun PremiumCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val clickable = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(
        modifier.fillMaxWidth().then(clickable).clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(.08f), RoundedCornerShape(24.dp))
            .padding(18.dp),
        content = content,
    )
}

@Composable
private fun EmptyCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, text: String, action: String, onClick: () -> Unit) {
    PremiumCard {
        IconTile(icon); Spacer(Modifier.height(18.dp)); Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp)); Text(text, color = MaterialTheme.colorScheme.onSurface.copy(.5f), lineHeight = 20.sp)
        Spacer(Modifier.height(18.dp)); Button(onClick = onClick, shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = PulseColors.Violet)) { Text(action) }
    }
}

@Composable
private fun IconTile(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(Brush.linearGradient(listOf(PulseColors.Violet.copy(.32f), PulseColors.Cyan.copy(.2f)))), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = PulseColors.Cyan)
    }
}

@Composable
private fun MetricCard(prefix: String, value: String, label: String, modifier: Modifier, color: Color) {
    Column(modifier.clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.onSurface.copy(.07f), RoundedCornerShape(22.dp)).padding(16.dp)) {
        Text(prefix, color = color, fontWeight = FontWeight.Bold); Spacer(Modifier.height(12.dp)); Text(value, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, maxLines = 1); Text(label, color = MaterialTheme.colorScheme.onSurface.copy(.38f), fontSize = 9.sp, letterSpacing = .8.sp)
    }
}

@Composable
private fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp, start = 22.dp, end = 22.dp, bottom = 28.dp),
        content = content,
    )
}

@Composable
private fun ScreenHeader(title: String, back: (() -> Unit)? = null, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().height(58.dp), verticalAlignment = Alignment.CenterVertically) {
        if (back != null) IconButton(onClick = back) { Icon(Icons.Outlined.ArrowBack, null) }
        Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
        trailing()
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun PulseMark(size: androidx.compose.ui.unit.Dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(size / 3)).background(Brush.linearGradient(listOf(PulseColors.Violet, PulseColors.Cyan))), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size * .62f)) {
            val y = this.size.height / 2
            val points = listOf(Offset(0f, y), Offset(this.size.width * .24f, y), Offset(this.size.width * .36f, y * .25f), Offset(this.size.width * .53f, y * 1.65f), Offset(this.size.width * .67f, y), Offset(this.size.width, y))
            points.zipWithNext().forEach { (a, b) -> drawLine(Color.White, a, b, 2.4.dp.toPx(), StrokeCap.Round) }
        }
    }
}

@Composable
private fun InlineBanner(message: String, close: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, PulseColors.Violet.copy(.32f), RoundedCornerShape(18.dp)).clickable(onClick = close).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { Icon(Icons.Outlined.Info, null, tint = PulseColors.Cyan); Spacer(Modifier.width(10.dp)); Text(message, modifier = Modifier.weight(1f), fontSize = 13.sp) }
}

@Composable private fun SectionLabel(text: String) { Spacer(Modifier.height(24.dp)); Text(text, color = MaterialTheme.colorScheme.onSurface.copy(.38f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp, modifier = Modifier.padding(start = 5.dp, bottom = 9.dp)) }
@Composable private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) = PremiumCard(content = content)
@Composable private fun DividerInset() = HorizontalDivider(Modifier.padding(start = 56.dp, top = 10.dp, bottom = 10.dp), color = MaterialTheme.colorScheme.onSurface.copy(.07f))

@Composable
private fun SettingSwitch(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, checked: Boolean, change: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = PulseColors.Cyan); Spacer(Modifier.width(16.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Medium); Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(.42f), fontSize = 12.sp) }; Switch(checked, change) }
}

@Composable
private fun SettingAction(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, click: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = click).padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = PulseColors.Cyan); Spacer(Modifier.width(16.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Medium); Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(.42f), fontSize = 12.sp) }; Icon(Icons.Outlined.ArrowBack, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface.copy(.25f)) }
}

@Composable
private fun ChoiceRow(title: String, selected: String, choices: List<Pair<String, String>>, change: (String) -> Unit) {
    Column { Text(title, fontWeight = FontWeight.Medium); Spacer(Modifier.height(11.dp)); Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { choices.forEach { (value, label) ->
        val active = selected == value
        Box(Modifier.weight(1f).clip(CircleShape).background(if (active) PulseColors.Violet.copy(.25f) else MaterialTheme.colorScheme.surfaceVariant).clickable { change(value) }.padding(vertical = 9.dp), contentAlignment = Alignment.Center) { Text(label, color = if (active) PulseColors.Cyan else MaterialTheme.colorScheme.onSurface.copy(.55f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
    } } }
}

@Composable private fun LinearLoading() { Spacer(Modifier.height(16.dp)); androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth(), color = PulseColors.Cyan) }

private fun perAppLabel(state: PulseUiState) = when (state.perAppMode) {
    SettingsManager.Keys.PER_APP_PROXY_INCLUDE -> "Только выбранные · ${state.selectedApps.size}"
    SettingsManager.Keys.PER_APP_PROXY_EXCLUDE -> "Кроме выбранных · ${state.selectedApps.size}"
    else -> "Все приложения"
}

private fun formatSpeed(bytes: Long): String = "${formatBytes(bytes)}/с"
private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> "%.2f ГБ".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576 -> "%.1f МБ".format(bytes / 1_048_576.0)
    bytes >= 1024 -> "%.0f КБ".format(bytes / 1024.0)
    else -> "$bytes Б"
}
