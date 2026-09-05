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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.semantics.Role
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.ImageView
import android.content.Intent
import android.net.Uri as AndroidUri
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
import app.pulse.vpn.data.isInfoMetadata
import io.nekohasekai.sfa.constant.Status
import kotlinx.coroutines.delay
import java.net.URI
import java.text.DateFormat
import java.nio.charset.StandardCharsets
import java.util.Base64
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
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(enabled = state.screen != Screen.HOME) {
        viewModel.navigate(if (state.screen == Screen.APPS || state.screen == Screen.STATS) Screen.SETTINGS else Screen.HOME)
    }
    var showImport by remember { mutableStateOf(false) }
    LaunchedEffect(state.message, state.importing, state.testingServers) {
        if (state.message != null && !state.importing && !state.testingServers) {
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
            PulseBackdrop(Modifier.fillMaxSize(), animated = state.liveEffects)
            AnimatedContent(targetState = state.screen, label = "screen") { screen ->
                when (screen) {
                    Screen.HOME -> HomeScreen(state, requestConnect, viewModel::stopVpn, { viewModel.navigate(Screen.ROUTES) }, { viewModel.navigate(Screen.PROFILES) }, { showImport = true }, viewModel::selectServer, { state.selectedProfile?.let(viewModel::updateProfile) }, viewModel::testServers)
                    Screen.ROUTES -> RoutesScreen(state, viewModel::selectServer, viewModel::testServers, viewModel::refreshSubscriptions, { showImport = true })
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
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp, start = 20.dp, end = 20.dp),
            ) {
                state.message?.let { InlineBanner(it, viewModel::clearMessage) }
            }
        }
    }
}

@Composable
private fun PulseBackdrop(modifier: Modifier = Modifier, animated: Boolean = true) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    if (!animated) {
        Canvas(modifier) {
            val width = size.width
            val height = size.height
            val primaryCenter = Offset(width * .16f, height * .2f)
            val secondaryCenter = Offset(width * .84f, height * .54f)
            drawCircle(Brush.radialGradient(listOf(primary.copy(.12f), Color.Transparent), center = primaryCenter, radius = width * .62f), width * .62f, primaryCenter)
            drawCircle(Brush.radialGradient(listOf(secondary.copy(.08f), Color.Transparent), center = secondaryCenter, radius = width * .52f), width * .52f, secondaryCenter)
        }
        return
    }
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
        drawCircle(Brush.radialGradient(listOf(primary.copy(.16f), Color.Transparent), center = violet, radius = width * .62f), width * .62f, violet)
        drawCircle(Brush.radialGradient(listOf(secondary.copy(.10f), Color.Transparent), center = cyan, radius = width * .52f), width * .52f, cyan)
    }
}

@Composable
private fun PulseNavigation(current: Screen, navigate: (Screen) -> Unit) {
    Row(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(26.dp)).background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(.16f), RoundedCornerShape(26.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(
            Triple(Screen.HOME, Icons.Outlined.Home, "Главная"),
            Triple(Screen.ROUTES, Icons.Outlined.Route, "Маршруты"),
            Triple(Screen.SETTINGS, Icons.Outlined.Tune, "Настройки"),
        ).forEach { (screen, icon, label) ->
            val selected = current == screen
            val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(20.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primary.copy(.12f) else Color.Transparent)
                    .selectable(selected = selected, role = Role.Tab, onClick = { navigate(screen) })
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
                Text(label, color = color, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun HomeScreen(
    state: PulseUiState,
    connect: () -> Unit,
    disconnect: () -> Unit,
    routes: () -> Unit,
    profiles: () -> Unit,
    addProfile: () -> Unit,
    selectServer: (VpnServer) -> Unit,
    refreshProfile: () -> Unit,
    testPings: () -> Unit,
) {
    val selected = state.servers.firstOrNull(VpnServer::selected) ?: state.servers.firstOrNull()
    var subscriptionExpanded by rememberSaveable { mutableStateOf(true) }
    PullToRefreshBox(
        isRefreshing = state.importing,
        onRefresh = { if (!state.importing && state.selectedProfile?.sourceUrl != null) refreshProfile() },
        modifier = Modifier.fillMaxSize(),
    ) {
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
                    brush = Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)),
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

        Spacer(Modifier.height(16.dp))
        AnimatedContent(
            targetState = state.vpnStatus to (state.selectedProfile != null),
            label = "connection-title",
        ) { (status, configured) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    when {
                        !configured -> "Нужен профиль"
                        status == Status.Started -> "Подключено"
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
                        status == Status.Started -> if (state.routingMode == "direct") "Трафик идёт напрямую" else "VPN активен · ${perAppLabel(state)}"
                        status == Status.Starting -> "Настраиваем безопасный туннель"
                        else -> "Один импульс до приватности"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                )
            }
        }
        PulseConnectButton(
            status = state.vpnStatus,
            configured = state.selectedProfile != null,
            animated = state.liveEffects,
            onClick = when {
                state.selectedProfile == null -> addProfile
                state.vpnStatus == Status.Started || state.vpnStatus == Status.Starting -> disconnect
                else -> connect
            },
        )
        selected?.let { server ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(.35f))
                    .clickable(onClick = routes).heightIn(min = 48.dp)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Route, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(server.tag, Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(server.delayMs?.let { "$it мс" } ?: "Сменить", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
        state.selectedProfile?.let { profile ->
            Spacer(Modifier.height(12.dp))
            SubscriptionHeaderCard(
                profile = profile,
                servers = state.servers,
                openProfiles = profiles,
                refresh = refreshProfile,
                refreshing = state.importing,
                expanded = subscriptionExpanded,
                toggleExpanded = { subscriptionExpanded = !subscriptionExpanded },
                testPings = testPings,
                testingPings = state.testingServers,
            )
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
        } else if (subscriptionExpanded) {
            Text("СЕРВЕРЫ", modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(.38f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            state.servers.filterNot(VpnServer::isInfoMetadata).sortedByDescending { it.selected }.take(3).forEach { server ->
                HomeServerRow(server, selectServer)
                Spacer(Modifier.height(8.dp))
            }
            if (state.servers.isNotEmpty()) ActionTile(Icons.Outlined.Route, "Все серверы · ${state.servers.size}", Modifier.fillMaxWidth(), onClick = routes)
            if (state.servers.none { !it.isInfoMetadata() }) {
                PremiumCard(onClick = routes) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(Icons.Outlined.Route)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Нет VPN-серверов", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text("Откройте маршруты для обновления", color = MaterialTheme.colorScheme.onSurface.copy(.45f), fontSize = 12.sp)
                        }
                        Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(.32f))
                    }
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
    expanded: Boolean,
    toggleExpanded: () -> Unit,
    testPings: () -> Unit,
    testingPings: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val expires = epochSeconds(profile.expireAt)?.takeIf { it > 0 }
    var clock by remember(expires) { mutableStateOf(System.currentTimeMillis()) }
    var showDescription by rememberSaveable(profile.id) { mutableStateOf(false) }
    LaunchedEffect(expires) {
        while (expires != null) { clock = System.currentTimeMillis(); delay(30_000) }
    }
    val daysLeft = expires?.let { ((it * 1000 - clock).coerceAtLeast(0) + 86_399_999) / 86_400_000 }
    val used = (profile.uploadBytes ?: 0L).coerceAtLeast(0) + (profile.downloadBytes ?: 0L).coerceAtLeast(0)
    val total = profile.totalBytes?.takeIf { it > 0 }
    val routeServers = servers.filterNot(VpnServer::isInfoMetadata)
    val support = profile.providerSupportUrl ?: profile.providerTelegram
    if (showDescription && profile.providerDescription != null) AlertDialog(
        onDismissRequest = { showDescription = false },
        title = { Text("Сообщение провайдера") },
        text = { SelectionContainer { Text(profile.providerDescription, modifier = Modifier.verticalScroll(rememberScrollState()), lineHeight = 23.sp) } },
        confirmButton = { TextButton(onClick = { showDescription = false }) { Text("Понятно") } },
    )
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(colors.primaryContainer.copy(.50f), colors.surface, colors.secondaryContainer.copy(.24f))))
            .border(1.dp, colors.primary.copy(.22f), RoundedCornerShape(28.dp)).padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PulseMark(42.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f).clickable(onClick = openProfiles)) {
                Text("ВАША ПОДПИСКА", color = colors.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                Spacer(Modifier.height(4.dp))
                Text(displayProfileName(profile), fontSize = 19.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = toggleExpanded) {
                Icon(if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, if (expanded) "Свернуть подписку" else "Развернуть подписку", tint = colors.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsMetric("СРОК", when { daysLeft == null -> "Без даты"; daysLeft == 0L -> "Истёк"; else -> "$daysLeft дн." }, Modifier.weight(1f))
            SettingsMetric("СЕРВЕРЫ", routeServers.size.toString(), Modifier.weight(1f))
            SettingsMetric("ОСТАЛОСЬ", total?.let { formatBytes((it - used).coerceAtLeast(0)) } ?: "Без лимита", Modifier.weight(1f))
        }
        if (total != null) {
            Spacer(Modifier.height(12.dp))
            androidx.compose.material3.LinearProgressIndicator(
                progress = { (used.toFloat() / total).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = if (used >= total) colors.error else colors.primary,
                trackColor = colors.onSurface.copy(.07f),
            )
            Spacer(Modifier.height(6.dp))
            Text("Использовано ${formatBytes(used)} из ${formatBytes(total)}", color = colors.onSurfaceVariant, fontSize = 11.sp)
        }
        AnimatedVisibility(expanded) {
            Column {
                profile.providerDescription?.takeIf(String::isNotBlank)?.let { description ->
                    Spacer(Modifier.height(16.dp))
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                            .background(colors.surface.copy(.6f)).clickable { showDescription = true }.padding(14.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Info, null, Modifier.size(16.dp), tint = colors.secondary)
                            Spacer(Modifier.width(7.dp))
                            Text("ОТ ПРОВАЙДЕРА", color = colors.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Outlined.ChevronRight, "Полное описание", Modifier.size(16.dp), tint = colors.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(description, color = colors.onSurface, fontSize = 14.sp, lineHeight = 21.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (support != null || profile.providerWebsite != null) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        support?.let { link -> ActionTile(Icons.Outlined.Send, "Поддержка", Modifier.weight(1f), onClick = { openExternal(context, link) }) }
                        profile.providerWebsite?.let { link -> ActionTile(Icons.Outlined.Language, "Кабинет", Modifier.weight(1f), onClick = { openExternal(context, link) }) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Обновлено ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(profile.updatedAt))}",
                    color = colors.onSurfaceVariant, fontSize = 11.sp,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionTile(Icons.Outlined.Refresh, "Обновить", Modifier.weight(1f), enabled = profile.sourceUrl != null && !refreshing, loading = refreshing, onClick = refresh)
            ActionTile(Icons.Outlined.Speed, "Проверить", Modifier.weight(1f), enabled = routeServers.isNotEmpty() && !testingPings, loading = testingPings, onClick = testPings)
        }
    }
}

@Composable
private fun ActionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(16.dp))
            .background(colors.primary.copy(if (enabled) .10f else .04f))
            .border(1.dp, colors.primary.copy(if (enabled) .16f else .06f), RoundedCornerShape(16.dp))
            .clickable(enabled = enabled && !loading, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(18.dp), color = colors.primary, strokeWidth = 2.dp)
        else Icon(icon, null, Modifier.size(18.dp), tint = colors.primary.copy(if (enabled) 1f else .4f))
        Spacer(Modifier.width(7.dp))
        Text(if (loading) "Проверяем…" else label, color = colors.onSurface.copy(if (enabled || loading) 1f else .4f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun displayProfileName(profile: VpnProfile): String = profile.name
private fun epochSeconds(value: Long?): Long? = value?.let { if (it > 100_000_000_000L) it / 1000L else it }
private fun openExternal(context: android.content.Context, link: String) {
    val uri = runCatching { URI(link) }.getOrNull() ?: return
    if (uri.scheme?.lowercase() !in setOf("http", "https") || uri.host.isNullOrBlank()) return
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, AndroidUri.parse(link))) }
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
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(.34f), MaterialTheme.colorScheme.secondary.copy(.18f)))), contentAlignment = Alignment.Center) {
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
        "poland" in normalized || "polska" in normalized || "поль" in normalized || "warsaw" in normalized -> "🇵🇱"
        "estonia" in normalized || "эстон" in normalized || "tallinn" in normalized -> "🇪🇪"
        "sweden" in normalized || "швед" in normalized || "stockholm" in normalized -> "🇸🇪"
        "romania" in normalized || "румын" in normalized || "bucha" in normalized -> "🇷🇴"
        "spain" in normalized || "испан" in normalized || "madrid" in normalized -> "🇪🇸"
        "italy" in normalized || "итал" in normalized || "rome" in normalized -> "🇮🇹"
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
private fun PulseConnectButton(status: Status, configured: Boolean, animated: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val active = status == Status.Started
    val moving = status == Status.Starting || status == Status.Stopping
    val accent = if (active) colors.secondary else colors.primary
    val pulse = if (animated && (active || moving)) {
        val transition = rememberInfiniteTransition(label = "connection-breath")
        val value by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2200), RepeatMode.Reverse), label = "connection-glow")
        value
    } else 0f
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .96f else 1f, tween(180), label = "connection-press")
    val haptic = LocalHapticFeedback.current
    val label = when {
        status == Status.Starting -> "Отменить"
        status == Status.Stopping -> "Завершаем…"
        active -> "Отключить"
        configured -> "Подключить"
        else -> "Добавить подписку"
    }
    Box(Modifier.fillMaxWidth().height(202.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(198.dp)) {
            drawCircle(Brush.radialGradient(listOf(accent.copy(.18f + pulse * .07f), Color.Transparent)))
            drawCircle(accent.copy(.10f), radius = size.minDimension * .48f, style = Stroke(1.dp.toPx()))
            drawCircle(accent.copy(.25f + pulse * .12f), radius = size.minDimension * (.40f + pulse * .018f), style = Stroke(1.dp.toPx()))
        }
        Column(
            Modifier.size(150.dp).graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(colors.primaryContainer, colors.surface)))
                .border(1.5.dp, accent.copy(.7f), CircleShape)
                .clickable(enabled = status != Status.Stopping, role = Role.Button, interactionSource = interaction, indication = null) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick()
                },
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
        ) {
            if (moving) CircularProgressIndicator(Modifier.size(36.dp), color = accent, strokeWidth = 2.dp)
            else Icon(if (configured) Icons.Outlined.PowerSettingsNew else Icons.Outlined.Add, null, Modifier.size(40.dp), tint = accent)
            Spacer(Modifier.height(14.dp))
            Text(label, color = colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RoutesScreen(
    state: PulseUiState,
    select: (VpnServer) -> Unit,
    test: () -> Unit,
    refresh: () -> Unit,
    add: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("all") }
    var details by remember { mutableStateOf<VpnServer?>(null) }
    val filtered = remember(state.servers, query, filter) {
        state.servers.filterNot(VpnServer::isInfoMetadata).filter { server ->
            server.tag.contains(query, ignoreCase = true) && when (filter) {
                "available" -> server.delayMs != null
                "slow" -> server.delayMs == null || server.delayMs >= 250
                else -> true
            }
        }
    }
    val allRoutes = remember(state.servers) { state.servers.filterNot(VpnServer::isInfoMetadata) }
    Column(
        Modifier.fillMaxSize().padding(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
            start = 20.dp,
            end = 20.dp,
        ),
    ) {
        ScreenHeader("Маршруты", trailing = {
            TextButton(onClick = test, enabled = state.servers.any { !it.isInfoMetadata() } && !state.testingServers) {
                if (state.testingServers) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Outlined.Speed, "Проверить задержку", modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(5.dp))
                Text(if (state.testingServers) "Проверяем…" else "Проверить все", fontSize = 12.sp)
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
        Spacer(Modifier.height(9.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RouteFilterChip("Все", filter == "all") { filter = "all" }
            RouteFilterChip("Доступные", filter == "available") { filter = "available" }
            RouteFilterChip("Медленные / нет ответа", filter == "slow") { filter = "slow" }
        }
        if (allRoutes.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            PingOverview(
                total = allRoutes.size,
                checked = allRoutes.count { it.delayMs != null },
                fastest = allRoutes.mapNotNull(VpnServer::delayMs).minOrNull(),
                running = state.testingServers,
                progress = state.pingCompleted to state.pingTotal,
                onClick = test,
            )
        }
        Spacer(Modifier.height(14.dp))
        PullToRefreshBox(
            isRefreshing = state.importing,
            onRefresh = { if (!state.importing) refresh() },
            state = rememberPullToRefreshState(),
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when {
                    state.selectedProfile == null -> item {
                        EmptyCard(Icons.Outlined.Route, "Маршрутов пока нет", "Добавьте подписку — список появится сразу после импорта.", "Добавить подписку", add)
                    }
                    state.servers.none { !it.isInfoMetadata() } -> item {
                        EmptyCard(Icons.Outlined.Info, "В профиле нет серверов", "Обновите подписку или добавьте другую.", "Добавить другой", add)
                    }
                    filtered.isEmpty() -> item {
                        EmptyCard(Icons.Outlined.Search, "Ничего не найдено", "Попробуйте изменить запрос или фильтр.", "Показать все", { query = ""; filter = "all" })
                    }
                    else -> items(filtered, key = VpnServer::tag) { server ->
                        ServerRow(server, { select(server) }, { details = server })
                    }
                }
            }
        }
    }
    details?.let { server -> ServerDetailsDialog(server, state.pingHistory[server.tag].orEmpty()) { details = null } }
}

@Composable
private fun RouteFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(.62f))
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(.42f) else MaterialTheme.colorScheme.onSurface.copy(.07f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(label, color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface.copy(.55f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PingOverview(
    total: Int,
    checked: Int,
    fastest: Int?,
    running: Boolean,
    progress: Pair<Int, Int>,
    onClick: () -> Unit,
) {
    val (done, expected) = progress
    val fraction = if (expected > 0) (done.toFloat() / expected).coerceIn(0f, 1f) else 0f
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = .78f))
            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = .14f), RoundedCornerShape(18.dp))
            .clickable(enabled = !running, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary.copy(.12f)), contentAlignment = Alignment.Center) {
            if (running) CircularProgressIndicator(Modifier.size(18.dp), color = MaterialTheme.colorScheme.secondary, strokeWidth = 2.dp)
            else Icon(Icons.Outlined.Speed, "Проверить пинг", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(if (running) "Проверяем серверы…" else "Диагностика маршрутов", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(
                if (running) "$done из $expected проверено" else "$checked из $total доступны · лучший ${fastest?.let { "$it мс" } ?: "—"}",
                color = MaterialTheme.colorScheme.onSurface.copy(.48f), fontSize = 11.sp,
            )
            if (running) {
                Spacer(Modifier.height(6.dp))
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(.10f),
                )
            }
        }
        if (!running) Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(.35f), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ServerRow(server: VpnServer, onClick: () -> Unit, onInfo: () -> Unit) {
    PremiumCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(
                    Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(.28f), MaterialTheme.colorScheme.secondary.copy(.16f))),
                ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Language, null, tint = MaterialTheme.colorScheme.secondary)
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
            Text(
                server.delayMs?.let { "${it} мс" } ?: "—",
                color = server.delayMs?.let { if (it < 250) PulseColors.Success else Color(0xFFFFB86B) }
                    ?: MaterialTheme.colorScheme.onSurface.copy(.38f),
                fontSize = 12.sp,
            )
            IconButton(onClick = onInfo, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Outlined.Info, "Детали сервера", tint = MaterialTheme.colorScheme.onSurface.copy(.52f), modifier = Modifier.size(17.dp))
            }
            if (server.selected) {
                Spacer(Modifier.width(10.dp))
                Icon(Icons.Outlined.Check, null, tint = PulseColors.Success)
            }
        }
    }
}

@Composable
private fun ServerDetailsDialog(server: VpnServer, history: List<Int>, close: () -> Unit) {
    val loss = if (history.isEmpty()) null else history.count { it < 0 } * 100 / history.size
    AlertDialog(
        onDismissRequest = close,
        confirmButton = { TextButton(onClick = close) { Text("Готово") } },
        title = { Text(server.tag, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailLine("Доступность", if (server.delayMs != null) "Доступен" else "Нет ответа")
                DetailLine("Задержка", server.delayMs?.let { "$it мс" } ?: "Не проверена")
                DetailLine("Протокол", server.type.uppercase())
                DetailLine("Адрес", server.address ?: "—")
                DetailLine("Порт", server.port?.toString() ?: "—")
                DetailLine("История", if (history.isEmpty()) "Пока нет замеров" else "${history.size} замеров")
                DetailLine("Потери", loss?.let { "$it%" } ?: "—")
            }
        },
    )
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(.52f), fontSize = 13.sp)
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    var pendingDelete by remember { mutableStateOf<VpnProfile?>(null) }
    pendingDelete?.let { profile ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Удалить профиль?") },
            text = { Text("${displayProfileName(profile)} и история проверок будут удалены с устройства. Для восстановления понадобится ссылка подписки.") },
            confirmButton = { TextButton(onClick = { pendingDelete = null; delete(profile) }) { Text("Удалить", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Отмена") } },
        )
    }
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
                            Text(displayProfileName(profile), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                            if (state.selectedProfile?.id == profile.id) Text("  ACTIVE", color = PulseColors.Success, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        val expiry = epochSeconds(profile.expireAt)?.takeIf { it > 0 }?.let { DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it * 1000)) }
                        Text(expiry?.let { "До $it" } ?: if (profile.sourceUrl != null) "Онлайн-подписка" else "Локальная конфигурация", color = MaterialTheme.colorScheme.onSurface.copy(.45f), fontSize = 12.sp)
                    }
                    if (profile.sourceUrl != null) IconButton(onClick = { update(profile) }, enabled = !state.importing) { Icon(Icons.Outlined.Refresh, "Обновить") }
                    IconButton(onClick = { pendingDelete = profile }, enabled = !state.importing) { Icon(Icons.Outlined.DeleteOutline, "Удалить", tint = MaterialTheme.colorScheme.error) }
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
            MetricCard("↓", formatBytes(state.traffic.downloadTotal), "ПОЛУЧЕНО", Modifier.weight(1f), MaterialTheme.colorScheme.secondary)
            MetricCard("↑", formatBytes(state.traffic.uploadTotal), "ОТПРАВЛЕНО", Modifier.weight(1f), MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SpeedGraph(samples: List<Long>) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    Canvas(Modifier.fillMaxWidth().height(170.dp)) {
        if (samples.size < 2) return@Canvas
        val peak = max(1L, samples.max()).toFloat()
        val step = size.width / (samples.size - 1)
        val points = samples.mapIndexed { index, value -> Offset(index * step, size.height - (value / peak) * size.height * .86f) }
        for (i in 0 until points.lastIndex) drawLine(
            brush = Brush.horizontalGradient(listOf(primary, secondary)),
            start = points[i], end = points[i + 1], strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun SettingsScreen(state: PulseUiState, viewModel: PulseViewModel, openVpnSettings: () -> Unit) {
    ScreenColumn {
        ScreenHeader("Настройки")
        Text("Ваш Pulse. Ваши правила.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))
        SettingsSnapshotCard(state, { viewModel.navigate(Screen.PROFILES) }, { viewModel.navigate(Screen.ROUTES) })
        SectionLabel("ОФОРМЛЕНИЕ")
        ThemeGallery(state, viewModel::setAccentTheme)
        Spacer(Modifier.height(12.dp))
        SettingsCard {
            SettingSwitch(Icons.Outlined.Tune, "Тёмное оформление", "Палитра всех экранов и карточек", state.darkTheme, viewModel::setDarkTheme)
            DividerInset()
            SettingSwitch(Icons.Outlined.AutoGraph, "Живой фон", "Мягкое свечение и пульсация подключения", state.liveEffects, viewModel::setLiveEffects)
        }
        SectionLabel("СОЕДИНЕНИЕ")
        SettingsCard {
            SettingSwitch(Icons.Outlined.Bolt, "Автоподключение", "Восстанавливать VPN после перезагрузки", state.autoConnect, viewModel::setAutoConnect)
            DividerInset()
            SettingSwitch(Icons.Outlined.Speed, "Выбирать быстрый сервер", "После проверки задержки", state.autoFastest, viewModel::setAutoFastest)
            DividerInset()
            SettingAction(Icons.Outlined.Lock, "Защита при обрыве", "Настроить постоянный VPN в Android", openVpnSettings)
        }
        SectionLabel("ТРАФИК И DNS")
        SettingsCard {
            ChoiceRow("Маршрутизация", state.routingMode, listOf("rules" to "По правилам", "global" to "Весь трафик", "direct" to "Напрямую"), viewModel::setRoutingMode)
            DividerInset()
            ChoiceRow("DNS-сервер", state.dnsMode, listOf("local" to "Из профиля", "cloudflare" to "Cloudflare", "google" to "Google"), viewModel::setDnsMode)
            DividerInset()
            SettingAction(Icons.Outlined.Apps, "Приложения", perAppLabel(state), { viewModel.navigate(Screen.APPS) })
        }
        Spacer(Modifier.height(12.dp))
        SettingsHintCard()
        SectionLabel("ПОДПИСКИ")
        SettingsCard {
            SettingSwitch(Icons.Outlined.Refresh, "Обновлять при открытии", "Получать свежие серверы и описание", state.refreshOnOpen, viewModel::setRefreshOnOpen)
            DividerInset()
            SettingAction(Icons.Outlined.Devices, "Управление профилями", "${state.profiles.size} профилей · ссылки и конфигурации", { viewModel.navigate(Screen.PROFILES) })
            DividerInset()
            ActionTile(Icons.Outlined.Refresh, "Обновить подписки", Modifier.fillMaxWidth(), enabled = !state.importing && state.profiles.any { it.sourceUrl != null }, loading = state.importing, onClick = { viewModel.refreshSubscriptions() })
        }
        SectionLabel("ДИАГНОСТИКА")
        SettingsCard {
            SettingAction(Icons.Outlined.Speed, "Проверка серверов", "Задержка, доступность и история", { viewModel.navigate(Screen.ROUTES) })
            DividerInset()
            SettingAction(Icons.Outlined.AutoGraph, "Статистика сессии", "Скорость и объём переданного трафика", { viewModel.navigate(Screen.STATS) })
        }
        Spacer(Modifier.height(28.dp))
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            PulseMark(32.dp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Pulse VPN", fontWeight = FontWeight.SemiBold)
                Text("Версия ${app.pulse.vpn.BuildConfig.VERSION_NAME}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.weight(1f))
            Text(viewModel.coreVersion(), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ThemeGallery(state: PulseUiState, select: (String) -> Unit) {
    val options = listOf("pulse" to "Pulse", "ocean" to "Ocean", "ember" to "Ember", "midnight" to "Midnight", "mono" to "Mono", "profile" to "Из подписки")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (value, name) ->
                    val active = state.accentTheme == value
                    val accent = if (value == "profile") state.selectedProfile?.themeHint ?: "pulse" else value
                    PulseTheme(state.darkTheme, accent) {
                        val colors = MaterialTheme.colorScheme
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(20.dp))
                                .background(colors.surface)
                                .border(if (active) 2.dp else 1.dp, if (active) colors.primary else colors.outline.copy(.15f), RoundedCornerShape(20.dp))
                                .selectable(selected = active, role = Role.RadioButton, onClick = { select(value) }).padding(12.dp),
                        ) {
                            Box(
                                Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(12.dp))
                                    .background(Brush.linearGradient(listOf(colors.background, colors.primaryContainer, colors.secondaryContainer))),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(Modifier.size(30.dp).clip(CircleShape).background(colors.primary.copy(.2f)).border(1.dp, colors.primary, CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.PowerSettingsNew, null, Modifier.size(17.dp), tint = colors.primary)
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                if (active) Icon(Icons.Outlined.Check, "Выбрано", Modifier.size(16.dp), tint = colors.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSnapshotCard(state: PulseUiState, openProfiles: () -> Unit, openRoutes: () -> Unit) {
    val routes = state.servers.filterNot(VpnServer::isInfoMetadata)
    val available = routes.count { it.delayMs != null }
    val selected = routes.firstOrNull(VpnServer::selected)
    PremiumCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(Icons.Outlined.Tune)
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text("СОСТОЯНИЕ PULSE", color = MaterialTheme.colorScheme.onSurface.copy(.46f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(3.dp))
                Text(state.selectedProfile?.let(::displayProfileName) ?: "Профиль не добавлен", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            StatusPill(state.vpnStatus)
        }
        Spacer(Modifier.height(15.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsMetric("МАРШРУТ", selected?.tag ?: "Не выбран", Modifier.weight(1.2f))
            SettingsMetric("СЕРВЕРЫ", if (routes.isEmpty()) "—" else "$available/${routes.size}", Modifier.weight(.8f))
            SettingsMetric("РЕЖИМ", routingLabel(state.routingMode), Modifier.weight(1f))
        }
        Spacer(Modifier.height(13.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = openProfiles, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(.16f), contentColor = MaterialTheme.colorScheme.secondary)) {
                Icon(Icons.Outlined.Devices, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Профили", fontSize = 12.sp)
            }
            Button(onClick = openRoutes, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary.copy(.14f), contentColor = MaterialTheme.colorScheme.secondary)) {
                Icon(Icons.Outlined.Route, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Маршруты", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SettingsMetric(title: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(.55f)).padding(horizontal = 10.dp, vertical = 9.dp)) {
        Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .5.sp, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SettingsHintCard() {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.secondary.copy(.08f)).border(1.dp, MaterialTheme.colorScheme.secondary.copy(.16f), RoundedCornerShape(18.dp)).padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(10.dp))
        Text("Изменения маршрутизации и DNS применятся при следующем подключении. Профиль и история пингов сохраняются автоматически.", color = MaterialTheme.colorScheme.onSurface.copy(.65f), fontSize = 11.sp, lineHeight = 16.sp)
    }
}

private fun routingLabel(value: String): String = when (value) {
    "global" -> "Весь VPN"
    "direct" -> "Напрямую"
    else -> "Правила"
}

@Composable
private fun AppsScreen(state: PulseUiState, back: () -> Unit, setMode: (Int) -> Unit, toggle: (String) -> Unit) {
    var search by rememberSaveable { mutableStateOf("") }
    val visibleApps = remember(state.apps, search) { state.apps.filter { it.label.contains(search, true) || it.packageName.contains(search, true) } }
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
                        .background(if (active) MaterialTheme.colorScheme.primary.copy(.25f) else MaterialTheme.colorScheme.surface)
                        .clickable { setMode(mode) }.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(label, color = if (active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface.copy(.5f), fontSize = 12.sp) }
            }
        }
        OutlinedTextField(search, { search = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp), singleLine = true, shape = RoundedCornerShape(18.dp), placeholder = { Text("Найти приложение") }, leadingIcon = { Icon(Icons.Outlined.Search, null) })
        LazyColumn(contentPadding = PaddingValues(14.dp, 8.dp, 14.dp, 32.dp)) {
            if (visibleApps.isEmpty()) item { Text("Приложения не найдены", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(visibleApps, key = ProfileRepository.AppEntry::packageName) { app ->
                Row(
                    Modifier.fillMaxWidth().clickable(enabled = state.perAppMode != SettingsManager.Keys.PER_APP_PROXY_DISABLED) { toggle(app.packageName) }.padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(app.packageName, app.label)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) { Text(app.label); Text(app.packageName, color = MaterialTheme.colorScheme.onSurface.copy(.38f), fontSize = 11.sp) }
                    androidx.compose.material3.Checkbox(checked = app.packageName in state.selectedApps && state.perAppMode != SettingsManager.Keys.PER_APP_PROXY_DISABLED, enabled = state.perAppMode != SettingsManager.Keys.PER_APP_PROXY_DISABLED, onCheckedChange = { toggle(app.packageName) })
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
            Button(onClick = { onImport(value) }, enabled = value.isNotBlank() && !loading, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White) else Text("Импортировать")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun StatusPill(status: Status) {
    val (text, color) = when (status) {
        Status.Started -> "В сети" to MaterialTheme.colorScheme.secondary
        Status.Starting, Status.Stopping -> "Ожидание" to MaterialTheme.colorScheme.primary
        Status.Stopped -> "Выключен" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(Modifier.clip(CircleShape).background(color.copy(.10f)).border(1.dp, color.copy(.25f), CircleShape).padding(horizontal = 9.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
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
        Spacer(Modifier.height(18.dp)); Button(onClick = onClick, shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text(action) }
    }
}

@Composable
private fun IconTile(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(.32f), MaterialTheme.colorScheme.secondary.copy(.2f)))), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.secondary)
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
    Box(Modifier.size(size).clip(RoundedCornerShape(size / 3)).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))), contentAlignment = Alignment.Center) {
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
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(.28f), RoundedCornerShape(20.dp))
            .padding(start = 14.dp, top = 11.dp, bottom = 11.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(10.dp))
        Text(message, modifier = Modifier.weight(1f), fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        IconButton(onClick = close, modifier = Modifier.size(38.dp)) {
            Icon(Icons.Outlined.Close, "Закрыть", modifier = Modifier.size(19.dp))
        }
    }
}

@Composable private fun SectionLabel(text: String) { Spacer(Modifier.height(20.dp)); Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, modifier = Modifier.padding(start = 5.dp, bottom = 9.dp)) }
@Composable private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) = PremiumCard(content = content)
@Composable private fun DividerInset() = HorizontalDivider(Modifier.padding(start = 56.dp, top = 10.dp, bottom = 10.dp), color = MaterialTheme.colorScheme.onSurface.copy(.07f))

@Composable
private fun SettingSwitch(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, checked: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).toggleable(value = checked, role = Role.Switch, onValueChange = change), verticalAlignment = Alignment.CenterVertically) {
        IconTile(icon)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 17.sp)
        }
        Switch(checked, null)
    }
}

@Composable
private fun SettingAction(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, click: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(role = Role.Button, onClick = click), verticalAlignment = Alignment.CenterVertically) {
        IconTile(icon)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 17.sp)
        }
        Icon(Icons.Outlined.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ChoiceRow(title: String, selected: String, choices: List<Pair<String, String>>, change: (String) -> Unit) {
    Column { Text(title, fontWeight = FontWeight.Medium); Spacer(Modifier.height(11.dp)); Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { choices.forEach { (value, label) ->
        val active = selected == value
        Box(Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(14.dp)).background(if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(.5f)).selectable(selected = active, role = Role.RadioButton, onClick = { change(value) }).padding(horizontal = 5.dp, vertical = 12.dp), contentAlignment = Alignment.Center) { Text(label, color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
    } } }
}

@Composable private fun LinearLoading() { Spacer(Modifier.height(16.dp)); androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.secondary) }

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
