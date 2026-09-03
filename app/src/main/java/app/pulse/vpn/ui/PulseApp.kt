package app.pulse.vpn.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
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
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Send
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.ImageView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pulse.vpn.PulseUiState
import app.pulse.vpn.PulseViewModel
import app.pulse.vpn.Screen
import app.pulse.vpn.R
import app.pulse.vpn.core.SettingsManager
import app.pulse.vpn.data.ProfileRepository
import app.pulse.vpn.data.VpnProfile
import app.pulse.vpn.data.VpnServer
import io.nekohasekai.sfa.constant.Status
import kotlinx.coroutines.delay
import java.net.URI
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.exp

@Composable
fun PulseApp(
    viewModel: PulseViewModel,
    requestConnect: () -> Unit,
    scanQr: () -> Unit,
    openVpnSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    var showImport by remember { mutableStateOf(false) }
    LaunchedEffect(state.message) {
        if (state.message?.startsWith("Подписка добавлена") == true) {
            delay(3200)
            viewModel.clearMessage()
        }
    }
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
            if (state.screen in setOf(Screen.HOME, Screen.ROUTES, Screen.SETTINGS)) {
                PulseNavigation(state.screen, viewModel::navigate)
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            PulseBackdrop(Modifier.fillMaxSize())
            AnimatedContent(targetState = state.screen, label = "screen") { screen ->
                when (screen) {
                    Screen.HOME -> HomeScreen(state, requestConnect, viewModel::stopVpn, { viewModel.navigate(Screen.ROUTES) }, { viewModel.navigate(Screen.PROFILES) }, { showImport = true }, viewModel::selectServer, { state.selectedProfile?.let(viewModel::updateProfile) })
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
                modifier = Modifier.align(Alignment.TopCenter).padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + if (state.screen == Screen.HOME) 148.dp else 12.dp, start = 20.dp, end = 20.dp),
            ) {
                state.message?.let { InlineBanner(it, viewModel::clearMessage) }
            }
        }
    }
}

@Composable
private fun PulseBackdrop(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "ambient-background")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000), RepeatMode.Reverse),
        label = "ambient-drift",
    )
    Canvas(modifier) {
        val width = size.width
        val height = size.height
        val violet = Offset(width * (.12f + drift * .16f), height * (.18f + drift * .05f))
        val cyan = Offset(width * (.92f - drift * .18f), height * (.58f - drift * .08f))
        drawCircle(Brush.radialGradient(listOf(PulseColors.Violet.copy(.16f), Color.Transparent), radius = width * .62f), width * .62f, violet)
        drawCircle(Brush.radialGradient(listOf(PulseColors.Cyan.copy(.10f), Color.Transparent), radius = width * .52f), width * .52f, cyan)
    }
}

@Composable
private fun PulseNavigation(current: Screen, navigate: (Screen) -> Unit) {
    Box(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.fillMaxWidth().height(68.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = .96f))
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = .07f), CircleShape)
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(
                Triple(Screen.HOME, Icons.Outlined.Home, "Главная"),
                Triple(Screen.ROUTES, Icons.Outlined.Route, "Маршруты"),
                Triple(Screen.SETTINGS, Icons.Outlined.Tune, "Настройки"),
            ).forEach { (screen, icon, label) ->
                val selected = current == screen
                Box(
                    Modifier.size(48.dp).clip(CircleShape)
                        .background(
                            if (selected) PulseColors.Violet.copy(alpha = .18f)
                            else Color.Transparent,
                        )
                        .clickable { navigate(screen) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon,
                        label,
                        tint = if (selected) PulseColors.Cyan
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = .42f),
                    )
                }
            }
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
    selectServer: (VpnServer) -> Unit,
    refreshProfile: () -> Unit,
) {
    val selected = state.servers.firstOrNull(VpnServer::selected) ?: state.servers.firstOrNull()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 14.dp,
            start = 20.dp,
            end = 20.dp,
            bottom = 8.dp,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.pulse_app_icon),
                contentDescription = "Pulse VPN",
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)),
            )
            Spacer(Modifier.width(11.dp))
            Text(
                "PULSE",
                style = MaterialTheme.typography.headlineMedium.copy(
                    brush = Brush.linearGradient(listOf(PulseColors.Violet, PulseColors.Cyan)),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                ),
            )
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = .07f), CircleShape)
                    .clickable(onClick = profiles),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Devices, "Профили", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f))
            }
        }

        Spacer(Modifier.height(38.dp))
        AnimatedContent(
            targetState = state.vpnStatus to (state.selectedProfile != null),
            label = "connection-title",
        ) { (status, configured) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    when {
                        !configured -> "Нужен профиль"
                        status == Status.Started -> "Защищено"
                        status == Status.Starting -> "Подключаем"
                        status == Status.Stopping -> "Завершаем"
                        else -> "Не подключено"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    when {
                        !configured -> "Добавьте ссылку или отсканируйте QR"
                        status == Status.Started -> "Сигнал стабилен · трафик защищён"
                        status == Status.Starting -> "Настраиваем безопасный туннель"
                        else -> "Один импульс до приватности"
                    },
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .48f),
                    fontSize = 14.sp,
                )
            }
        }
        PulseConnectButton(
            status = state.vpnStatus,
            configured = state.selectedProfile != null,
            onClick = when {
                state.selectedProfile == null -> addProfile
                state.vpnStatus == Status.Started || state.vpnStatus == Status.Starting -> disconnect
                else -> connect
            },
        )
        state.selectedProfile?.let { profile ->
            Spacer(Modifier.height(12.dp))
            SubscriptionHeaderCard(profile, state.servers, profiles, refreshProfile, state.importing)
        }
        AnimatedVisibility(visible = state.vpnStatus == Status.Started) {
            Row(
                Modifier.fillMaxWidth().height(56.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompactMetric("↓", formatSpeed(state.traffic.downloadPerSecond), "Загрузка")
                Spacer(Modifier.width(34.dp))
                CompactMetric("↑", formatSpeed(state.traffic.uploadPerSecond), "Отдача")
            }
        }
        Spacer(Modifier.height(22.dp))
        if (state.selectedProfile == null) {
            PremiumCard(onClick = addProfile) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Outlined.Add)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Добавить VPN-профиль", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Spacer(Modifier.height(3.dp))
                        Text("QR, ссылка или конфигурация", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .45f), fontSize = 12.sp)
                    }
                    Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .32f))
                }
            }
        } else {
            Text("СЕРВЕРЫ", modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(.38f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            state.servers.forEach { server ->
                HomeServerRow(server, selectServer)
                Spacer(Modifier.height(8.dp))
            }
            if (state.servers.isEmpty()) {
                PremiumCard(onClick = routes) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(Icons.Outlined.Route)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Серверы загружаются", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text("Откройте маршруты для обновления", color = MaterialTheme.colorScheme.onSurface.copy(.45f), fontSize = 12.sp)
                        }
                        Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(.32f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscriptionHeaderCard(
    profile: VpnProfile,
    servers: List<VpnServer>,
    openProfiles: () -> Unit,
    refresh: () -> Unit,
    refreshing: Boolean,
) {
    val expires = profile.expireAt?.takeIf { it > 0 }
    val daysLeft = expires?.let {
        TimeUnit.MILLISECONDS.toDays((it * 1000L - System.currentTimeMillis()).coerceAtLeast(0L)).toInt()
    }
    val used = (profile.uploadBytes ?: 0L) + (profile.downloadBytes ?: 0L)
    val ping = servers.mapNotNull(VpnServer::delayMs).minOrNull()
    val source = profile.sourceUrl?.let { runCatching { URI(it).host }.getOrNull() }
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF29245E).copy(.94f), Color(0xFF123F54).copy(.94f)),
                ),
            )
            .border(1.dp, PulseColors.Violet.copy(.32f), RoundedCornerShape(26.dp))
            .clickable(onClick = openProfiles)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PulseMark(42.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(profile.name, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (profile.sourceUrl != null) "Удалённая подписка" else "Локальная конфигурация",
                    color = Color.White.copy(.62f),
                    fontSize = 12.sp,
                )
                Text(
                    "Обновлено ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(profile.updatedAt))}",
                    color = Color.White.copy(.46f),
                    fontSize = 10.sp,
                )
            }
            IconButton(onClick = refresh, enabled = !refreshing, modifier = Modifier.size(38.dp)) {
                if (refreshing) CircularProgressIndicator(Modifier.size(17.dp), color = Color.White, strokeWidth = 2.dp)
                else Icon(Icons.Outlined.Refresh, "Обновить", tint = Color.White.copy(.7f), modifier = Modifier.size(19.dp))
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = Color.White.copy(.58f))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            source?.let { "Источник: $it" } ?: "Конфигурация добавлена вручную",
            color = Color.White.copy(.55f),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = openProfiles,
                modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(.10f)),
            ) {
                Icon(Icons.Outlined.Info, "Информация о подписке", tint = Color.White.copy(.82f), modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = openProfiles,
                modifier = Modifier.size(36.dp).clip(CircleShape).background(PulseColors.Cyan.copy(.15f)),
            ) {
                Icon(Icons.Outlined.Send, "Telegram", tint = PulseColors.Cyan, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SubscriptionMeta(
                title = "СРОК",
                value = when {
                    daysLeft == null -> "Без даты"
                    daysLeft == 0 -> "Истёк"
                    else -> "$daysLeft дн."
                },
                modifier = Modifier.weight(1f),
            )
            SubscriptionMeta(
                title = "ТРАФИК",
                value = profile.totalBytes?.let { "${formatBytes(used)} / ${formatBytes(it)}" } ?: "Без лимита",
                modifier = Modifier.weight(1f),
            )
            SubscriptionMeta(
                title = "ПИНГ",
                value = ping?.let { "$it мс" } ?: "—",
                modifier = Modifier.weight(1f),
            )
        }
        expires?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                "Действует до ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it * 1000L))}",
                color = Color.White.copy(.66f),
                fontSize = 12.sp,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            if (servers.isNotEmpty()) "Выберите любую страну ниже — маршрут применится при подключении"
            else "Добавьте серверы из ссылки подписки",
            color = Color.White.copy(.62f),
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
    }
}

@Composable
private fun SubscriptionMeta(title: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(.14f)).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(title, color = Color.White.copy(.48f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun HomeServerRow(server: VpnServer, select: (VpnServer) -> Unit) {
    PremiumCard(onClick = { select(server) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(PulseColors.Violet.copy(.34f), PulseColors.Cyan.copy(.18f)))), contentAlignment = Alignment.Center) {
                Text(countryFlag(server.tag), fontSize = 21.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(server.tag, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(listOfNotNull(server.type.uppercase(), server.address).joinToString(" · "), color = MaterialTheme.colorScheme.onSurface.copy(.42f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            server.delayMs?.let { Text("${it} мс", color = if (it < 250) PulseColors.Success else Color(0xFFFFB86B), fontSize = 11.sp) }
            Spacer(Modifier.width(8.dp))
            if (server.selected) Box(Modifier.size(7.dp).clip(CircleShape).background(PulseColors.Success))
            Spacer(Modifier.width(7.dp))
            Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(.3f), modifier = Modifier.size(19.dp))
        }
    }
}

private fun countryFlag(tag: String): String {
    val normalized = tag.lowercase()
    return when {
        "france" in normalized || "франц" in normalized -> "🇫🇷"
        "finland" in normalized || "фин" in normalized -> "🇫🇮"
        "germany" in normalized || "герман" in normalized || "de" == normalized -> "🇩🇪"
        "netherlands" in normalized || "нидер" in normalized || "голланд" in normalized -> "🇳🇱"
        "usa" in normalized || "united states" in normalized || "амер" in normalized -> "🇺🇸"
        "uk" in normalized || "britain" in normalized || "англ" in normalized -> "🇬🇧"
        "singapore" in normalized || "сингапур" in normalized -> "🇸🇬"
        else -> "🌐"
    }
}

@Composable
private fun CompactMetric(icon: String, value: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon, color = PulseColors.Success, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(value, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .4f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun PulseConnectButton(status: Status, configured: Boolean, onClick: () -> Unit) {
    val active = status == Status.Started
    val moving = status == Status.Starting || status == Status.Stopping
    val infinite = rememberInfiniteTransition(label = "heartbeat")
    val phase by infinite.animateFloat(
        0f,
        1f,
        infiniteRepeatable(tween(if (moving) 760 else 1320), RepeatMode.Restart),
        label = "heartbeat-phase",
    )
    fun bump(center: Float, width: Float, amplitude: Float): Float {
        val x = (phase - center) / width
        return amplitude * exp((-x * x).toDouble()).toFloat()
    }
    val beat = if (active || moving) {
        bump(.18f, .055f, 1f) + bump(.31f, .075f, .58f) + bump(.68f, .14f, .18f)
    } else 0f
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) .96f else 1f, tween(140), label = "press")
    val haptic = LocalHapticFeedback.current

    Box(Modifier.fillMaxWidth().height(272.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(272.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            repeat(3) { index ->
                val base = size.minDimension * (.31f + index * .085f)
                drawCircle(
                    color = (if (active) PulseColors.Success else PulseColors.Violet)
                        .copy(alpha = if (active || moving) .18f - index * .035f else .055f),
                    radius = base * (1f + beat * (.018f + index * .008f)),
                    center = center,
                    style = Stroke(width = 1.25.dp.toPx()),
                )
            }
        }
        Box(
            Modifier.size(166.dp).graphicsLayer {
                scaleX = pressScale * (1f + beat * .012f)
                scaleY = pressScale * (1f + beat * .012f)
                shadowElevation = if (active) 30.dp.toPx() else 16.dp.toPx()
                shape = CircleShape
                clip = true
            }.background(
                Brush.linearGradient(
                    if (active) listOf(Color(0xFF19C8A1), PulseColors.Success)
                    else listOf(PulseColors.Violet, PulseColors.Cyan),
                ),
            ).border(1.dp, Color.White.copy(alpha = .24f), CircleShape)
                .clickable(interactionSource = interaction, indication = null) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (!moving) onClick()
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (moving) {
                    CircularProgressIndicator(Modifier.size(30.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(
                        if (active) Icons.Outlined.AutoGraph
                        else if (configured) Icons.Outlined.PowerSettingsNew
                        else Icons.Outlined.Add,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }
                Spacer(Modifier.height(9.dp))
                Text(
                    when {
                        active -> "В ПУЛЬСЕ"
                        moving && status == Status.Starting -> "СОЕДИНЯЕМ"
                        moving -> "ЗАВЕРШАЕМ"
                        configured -> "ПОДКЛЮЧИТЬ"
                        else -> "ДОБАВИТЬ КЛЮЧ"
                    },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.1.sp,
                )
            }
        }
    }
}

@Composable
private fun RoutesScreen(state: PulseUiState, select: (VpnServer) -> Unit, test: () -> Unit, add: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(state.servers, query) {
        state.servers.filter { it.tag.contains(query, ignoreCase = true) }
    }
    Column(
        Modifier.fillMaxSize().padding(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
            start = 20.dp,
            end = 20.dp,
        ),
    ) {
        ScreenHeader("Маршруты", trailing = {
            IconButton(onClick = test, enabled = state.servers.isNotEmpty()) {
                Icon(Icons.Outlined.Speed, "Проверить задержку")
            }
            IconButton(onClick = add) { Icon(Icons.Outlined.Add, "Добавить") }
        })
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = CircleShape,
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            placeholder = { Text("Название сервера") },
        )
        Spacer(Modifier.height(14.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when {
                state.selectedProfile == null -> item {
                    EmptyCard(Icons.Outlined.Route, "Маршрутов пока нет", "Добавьте подписку — список появится сразу после импорта.", "Добавить подписку", add)
                }
                state.servers.isEmpty() -> item {
                    EmptyCard(Icons.Outlined.Info, "В профиле нет серверов", "Обновите подписку или добавьте другую.", "Добавить другой", add)
                }
                filtered.isEmpty() -> item {
                    EmptyCard(Icons.Outlined.Search, "Ничего не найдено", "Попробуйте изменить запрос.", "Очистить", { query = "" })
                }
                else -> items(filtered, key = VpnServer::tag) { server ->
                    ServerRow(server, { select(server) })
                }
            }
        }
    }
}

@Composable
private fun ServerRow(server: VpnServer, onClick: () -> Unit) {
    PremiumCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(
                    Brush.linearGradient(listOf(PulseColors.Violet.copy(.28f), PulseColors.Cyan.copy(.16f))),
                ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Language, null, tint = PulseColors.Cyan)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(server.tag, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(server.type.uppercase(), server.address).joinToString(" · "),
                    color = MaterialTheme.colorScheme.onSurface.copy(.42f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (server.delayMs != null) {
                Text(
                    "${server.delayMs} мс",
                    color = if (server.delayMs < 250) PulseColors.Success else Color(0xFFFFB86B),
                    fontSize = 12.sp,
                )
            }
            if (server.selected) {
                Spacer(Modifier.width(10.dp))
                Icon(Icons.Outlined.Check, null, tint = PulseColors.Success)
            }
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
            SettingSwitch(Icons.Outlined.Refresh, "Обновлять при запуске", "Проверять удалённые подписки при открытии", state.refreshOnOpen, viewModel::setRefreshOnOpen)
            DividerInset()
            SettingAction(Icons.Outlined.Lock, "Kill switch", "Always-on VPN и блокировка без VPN", openVpnSettings)
            DividerInset()
            SettingAction(Icons.Outlined.Apps, "Маршрутизация приложений", perAppLabel(state), { viewModel.navigate(Screen.APPS) })
        }
        SectionLabel("МАРШРУТИЗАЦИЯ")
        SettingsCard {
            SettingAction(Icons.Outlined.Refresh, "Обновить подписки", "Проверить все ссылки сейчас", { viewModel.refreshSubscriptions() })
            DividerInset()
            ChoiceRow("Режим трафика", state.routingMode, listOf("rules" to "Правила", "global" to "Весь VPN", "direct" to "Напрямую"), viewModel::setRoutingMode)
            DividerInset()
            ChoiceRow("DNS", state.dnsMode, listOf("local" to "Из профиля", "cloudflare" to "Cloudflare", "google" to "Google"), viewModel::setDnsMode)
        }
        SectionLabel("ВИД")
        SettingsCard { SettingSwitch(Icons.Outlined.Tune, "Тёмная тема", "Фирменная тема Pulse", state.darkTheme, viewModel::setDarkTheme) }
        SectionLabel("О ПРИЛОЖЕНИИ")
        SettingsCard {
            SettingAction(Icons.Outlined.Info, "Pulse VPN 0.5", "Kotlin · Compose · sing-box", {})
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
                    AppIcon(app.packageName, app.label)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) { Text(app.label); Text(app.packageName, color = MaterialTheme.colorScheme.onSurface.copy(.38f), fontSize = 11.sp) }
                    androidx.compose.material3.Checkbox(checked = app.packageName in state.selectedApps, onCheckedChange = { toggle(app.packageName) })
                }
            }
        }
    }
}

@Composable
private fun AppIcon(packageName: String, label: String) {
    val context = LocalContext.current
    val drawable = remember(packageName) { runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull() }
    if (drawable == null) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Text(label.take(1).uppercase(), fontWeight = FontWeight.Bold)
        }
    } else {
        AndroidView(
            factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.CENTER_INSIDE; setImageDrawable(drawable) } },
            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)),
        )
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
                Text(
                    "Ссылка провайдера, QR или конфигурация. Pulse автоматически определит формат.",
                    color = MaterialTheme.colorScheme.onSurface.copy(.52f),
                    lineHeight = 20.sp,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 8,
                    shape = RoundedCornerShape(18.dp),
                    placeholder = { Text("https://…") },
                )
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
        modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).then(clickable)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .58f),
                        MaterialTheme.colorScheme.surface.copy(alpha = .92f),
                    ),
                ),
            )
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
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .98f))
            .border(1.dp, PulseColors.Violet.copy(.28f), RoundedCornerShape(20.dp))
            .padding(start = 14.dp, top = 11.dp, bottom = 11.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Info, null, tint = PulseColors.Cyan, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(10.dp))
        Text(message, modifier = Modifier.weight(1f), fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        IconButton(onClick = close, modifier = Modifier.size(38.dp)) {
            Icon(Icons.Outlined.Close, "Закрыть", modifier = Modifier.size(19.dp))
        }
    }
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
    Row(Modifier.fillMaxWidth().clickable(onClick = click).padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = PulseColors.Cyan); Spacer(Modifier.width(16.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Medium); Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(.42f), fontSize = 12.sp) }; Icon(Icons.Outlined.ChevronRight, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface.copy(.25f)) }
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
