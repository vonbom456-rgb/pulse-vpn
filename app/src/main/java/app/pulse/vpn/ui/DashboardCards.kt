package app.pulse.vpn.ui

import android.os.SystemClock
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pulse.vpn.PulseUiState
import app.pulse.vpn.data.SubscriptionUsage
import app.pulse.vpn.data.VpnProfile
import app.pulse.vpn.data.VpnServer
import io.nekohasekai.sfa.constant.Status
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** A compact control centre: real service state, one primary action, one selected route. */
@Composable
internal fun ConnectionCard(state: PulseUiState, connect: () -> Unit, disconnect: () -> Unit, add: () -> Unit, routes: () -> Unit, stats: () -> Unit, refresh: () -> Unit, profiles: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val active = state.vpnStatus == Status.Started
    val moving = state.vpnStatus in listOf(Status.Starting, Status.Stopping)
    val configured = state.selectedProfile != null
    val blocked = state.selectedProfile?.issue?.blocksConnection == true
    val remote = state.selectedProfile?.sourceUrl != null
    val server = state.servers.firstOrNull { it.selected }
    val large = LocalDensity.current.fontScale > 1.3f
    val title = when {
        !configured -> "Начнём с подписки"
        active -> "Подключено"
        state.vpnStatus == Status.Starting -> "Подключаем"
        state.vpnStatus == Status.Stopping -> "Завершаем"
        blocked -> "Подписка недоступна"
        else -> "VPN отключён"
    }
    val label = when {
        !configured -> "Добавить подписку"
        active -> "Отключить"
        state.vpnStatus == Status.Starting -> "Отменить"
        state.vpnStatus == Status.Stopping -> "Завершаем…"
        blocked -> if (remote) "Обновить" else "Выбрать подписку"
        else -> "Подключить"
    }
    var elapsed by remember { mutableLongStateOf(0) }
    LaunchedEffect(active, state.connectedSinceElapsedMs) {
        elapsed = 0
        while (active && state.connectedSinceElapsedMs != null) {
            elapsed = ((SystemClock.elapsedRealtime() - state.connectedSinceElapsedMs) / 1000).coerceAtLeast(0)
            delay(1000)
        }
    }
    val haptic = LocalHapticFeedback.current
    val action = {
        if (state.options.bool("haptics")) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        when { !configured -> add(); active || state.vpnStatus == Status.Starting -> disconnect(); blocked -> if (remote) refresh() else profiles(); else -> connect() }
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(colors.primaryContainer.copy(.48f), colors.surface, colors.secondaryContainer.copy(.24f))))
            .border(1.dp, colors.primary.copy(.22f), RoundedCornerShape(28.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(if (active) colors.secondary else colors.onSurfaceVariant))
            Text(if (active) "VPN АКТИВЕН" else "СОЕДИНЕНИЕ", Modifier.weight(1f).padding(start = 7.dp), fontSize = 10.sp, letterSpacing = 1.1.sp, fontWeight = FontWeight.Bold, color = colors.onSurfaceVariant)
            if (active) Text(String.format(Locale.ROOT, "%02d:%02d:%02d", elapsed / 3600, elapsed / 60 % 60, elapsed % 60), fontSize = 12.sp, color = colors.secondary)
            else Icon(Icons.Outlined.Shield, null, Modifier.size(18.dp), tint = colors.primary)
        }
        val caption = when {
            !configured -> "Вставьте ссылку подписки или отсканируйте QR."
            blocked && !active && !moving -> "Подписка сохранена. Причина и помощь — в карточке ниже."
            state.settingsPending -> "Есть изменения для следующего подключения"
            state.vpnStatus == Status.Starting -> "Запускаем VPN. Можно отменить."
            active && state.routingMode == "direct" -> "Режим напрямую: трафик без VPN"
            active -> "Туннель работает · " + when (state.perAppMode) { 1 -> "выбранные приложения"; 2 -> "с исключениями"; else -> "все приложения" }
            else -> "Нажмите кнопку для подключения"
        }
        val text: @Composable (Modifier) -> Unit = { modifier ->
            Column(modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(title, fontSize = 20.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold)
                Text(caption, fontSize = 12.sp, lineHeight = 17.sp, color = colors.onSurfaceVariant)
            }
        }
        val dial: @Composable () -> Unit = {
            Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
                val pulse = if (state.liveEffects && active) {
                    val transition = rememberInfiniteTransition(label = "connected-glow")
                    val value by transition.animateFloat(.15f, .34f, infiniteRepeatable(tween(2200), RepeatMode.Reverse), label = "glow")
                    value
                } else .16f
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(Brush.radialGradient(listOf(colors.primary.copy(pulse), Color.Transparent)))
                    drawCircle(colors.primary.copy(.24f), style = Stroke(1.dp.toPx()))
                }
                Box(Modifier.size(76.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(if (active) colors.secondary else colors.primary, colors.primaryContainer)))
                    .border(1.dp, colors.primary.copy(.7f), CircleShape)
                    .clickable(enabled = state.vpnStatus != Status.Stopping && !state.importing, role = Role.Button, onClickLabel = label, onClick = action),
                    contentAlignment = Alignment.Center) {
                    if (moving) CircularProgressIndicator(Modifier.size(30.dp), color = colors.onPrimaryContainer, strokeWidth = 2.dp)
                    else Icon(if (blocked) Icons.Outlined.Refresh else if (configured) Icons.Outlined.PowerSettingsNew else Icons.Outlined.Add, label, Modifier.size(36.dp), tint = colors.onPrimaryContainer)
                }
            }
        }
        if (large) {
            text(Modifier.fillMaxWidth())
            FilledTonalButton(action, Modifier.fillMaxWidth().heightIn(min = 56.dp), enabled = !state.importing && state.vpnStatus != Status.Stopping) { Text(label) }
        } else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            text(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) { dial(); Text(label, color = colors.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(enabled = !state.importing && state.vpnStatus != Status.Stopping, onClick = action).padding(top = 4.dp, bottom = 8.dp)) }
        }
        if (server != null) {
            HorizontalDivider(color = colors.outlineVariant.copy(.5f))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = routes).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(countryFlag(server.tag), fontSize = 25.sp)
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(server.tag, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(server.type.uppercase() + " · Выбранный сервер", fontSize = 10.sp, color = colors.onSurfaceVariant)
                }
                server.delayMs?.let { Text("$it мс", fontSize = 11.sp, color = colors.secondary) }
                Icon(Icons.Outlined.ChevronRight, "Сменить сервер", Modifier.size(20.dp), tint = colors.onSurfaceVariant)
            }
        }
        if (active) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.surface.copy(.65f)).clickable(onClick = stats).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniReading("↓ Загрузка", formatSpeed(state.traffic.downloadPerSecond), Modifier.weight(1f))
                MiniReading("↑ Отдача", formatSpeed(state.traffic.uploadPerSecond), Modifier.weight(1f))
            }
        }
    }
}

@Composable private fun MiniReading(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 3.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SubscriptionCard(profile: VpnProfile, servers: List<VpnServer>, openProfiles: () -> Unit, refresh: () -> Unit, refreshing: Boolean, expanded: Boolean, toggleExpanded: () -> Unit, testPings: () -> Unit, testingPings: Boolean, cancelPings: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    var now by remember(profile.id) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(profile.expireAt) { while (true) { now = System.currentTimeMillis(); delay(30_000) } }
    val usage = SubscriptionUsage.from(profile, now)
    var descriptionOpen by rememberSaveable(profile.id) { mutableStateOf(false) }
    val support = profile.providerSupportUrl ?: profile.providerTelegram
    val large = LocalDensity.current.fontScale > 1.3f
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(colors.surface)
        .border(1.dp, colors.outlineVariant.copy(.6f), RoundedCornerShape(24.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(colors.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Layers, null, Modifier.size(20.dp), tint = colors.onPrimaryContainer)
            }
            Column(Modifier.weight(1f).clickable(onClick = openProfiles).padding(start = 10.dp)) {
                Text("ВАША ПОДПИСКА", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp, color = colors.primary)
                Text(profile.name, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (!expanded) profile.issue?.let { Text(it.title, color = colors.error, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            IconButton(toggleExpanded) { Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, if (expanded) "Свернуть подписку" else "Развернуть подписку") }
        }
        androidx.compose.animation.AnimatedVisibility(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                profile.issue?.let { issue ->
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.errorContainer.copy(.55f)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(issue.title, color = colors.onErrorContainer, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(issue.message, color = colors.onErrorContainer, fontSize = 12.sp, lineHeight = 17.sp)
                        Text(if (issue.blocksConnection) issue.hint else "Сохранённые серверы доступны. Можно подключаться и повторить обновление позже.", color = colors.onErrorContainer, fontSize = 11.sp, lineHeight = 16.sp)
                    }
                }
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.surfaceContainerLow).padding(horizontal = 10.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiniReading("Срок подписки", when (usage.daysLeft) { null -> "Не указан"; 0L -> "Истёк"; else -> "${usage.daysLeft} дн." }, Modifier.weight(1f))
                    MiniReading("Остаток трафика", usage.remaining?.let(::formatBytes) ?: if (profile.totalBytes == 0L) "Без лимита" else "Не указан", Modifier.weight(1f))
                }
                if (usage.daysLeft == 0L || usage.exhausted) Text(if (usage.exhausted) "Лимит трафика исчерпан" else "Продлите подписку в кабинете или обратитесь в поддержку", fontSize = 12.sp, color = colors.error)
                FlowRow(Modifier.fillMaxWidth(), maxItemsInEachRow = if (large) 2 else 4, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SubscriptionAction(Icons.Outlined.Refresh, "Обновить", Modifier.weight(1f), profile.sourceUrl != null && !refreshing, refreshing, refresh)
                    SubscriptionAction(if (testingPings) Icons.Outlined.Close else Icons.Outlined.Speed, if (testingPings) "Отмена" else "Пинг", Modifier.weight(1f), testingPings || servers.isNotEmpty() && !refreshing, false, if (testingPings) cancelPings else testPings)
                    SubscriptionAction(Icons.Outlined.Send, "Поддержка", Modifier.weight(1f), support != null, false) { support?.let { openExternal(context, it) } }
                    SubscriptionAction(Icons.Outlined.Language, "Кабинет", Modifier.weight(1f), profile.providerWebsite != null, false) { profile.providerWebsite?.let { openExternal(context, it) } }
                }
                profile.providerDescription?.takeIf(String::isNotBlank)?.let { description ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(10.dp)).clickable { descriptionOpen = true }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(3.dp).height(28.dp).clip(CircleShape).background(colors.secondary))
                        Text(description, Modifier.weight(1f).padding(horizontal = 10.dp), fontSize = 12.sp, lineHeight = 17.sp, maxLines = if (expanded) 2 else 1, overflow = TextOverflow.Ellipsis)
                        Icon(Icons.Outlined.Info, null, Modifier.size(16.dp), tint = colors.onSurfaceVariant)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    usage.fraction?.let { fraction ->
                        LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape), color = if (usage.exhausted) colors.error else colors.secondary, trackColor = colors.surfaceContainerHighest)
                        Text("Использовано ${formatBytes(usage.used)} из ${formatBytes(usage.limit!!)}", fontSize = 10.sp, color = colors.onSurfaceVariant)
                    }
                    Text(if (profile.issue?.blocksConnection == true) "Подписка сохранена · Обновите её после восстановления доступа" else "${servers.size} серверов · Обновлено ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(profile.updatedAt))}", fontSize = 10.sp, color = colors.onSurfaceVariant)
                }
            }
        }
    }
    if (descriptionOpen) AlertDialog(onDismissRequest = { descriptionOpen = false }, title = { Text("О подписке") },
        text = { SelectionContainer { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(profile.name, fontWeight = FontWeight.SemiBold)
            Text(profile.providerDescription.orEmpty(), lineHeight = 23.sp)
        } } }, confirmButton = { TextButton({ descriptionOpen = false }) { Text("Понятно") } })
}

@Composable private fun SubscriptionAction(icon: ImageVector, label: String, modifier: Modifier, enabled: Boolean, loading: Boolean, action: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(colors.primary.copy(if (enabled) .08f else .025f))
        .clickable(enabled = enabled, role = Role.Button, onClick = action).heightIn(min = 52.dp).padding(horizontal = 2.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        if (loading) CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp)
        else Icon(icon, null, Modifier.size(19.dp), tint = colors.primary.copy(if (enabled) 1f else .35f))
        Text(label, fontSize = 10.sp, textAlign = TextAlign.Center, color = colors.onSurface.copy(if (enabled || loading) 1f else .4f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ServerPickerSheet(state: PulseUiState, select: (VpnServer) -> Unit, dismiss: () -> Unit, allRoutes: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val colors = MaterialTheme.colorScheme
    val visible = remember(state.servers, state.favorites, query) {
        state.servers.filter { it.tag.contains(query.trim(), true) }
            .sortedWith(compareByDescending<VpnServer> { it.selected }.thenByDescending { it.tag in state.favorites })
    }
    ModalBottomSheet(onDismissRequest = dismiss, containerColor = colors.surface, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Быстрый выбор", style = MaterialTheme.typography.titleLarge)
            Text(state.selectedProfile?.name.orEmpty(), color = colors.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(16.dp),
                placeholder = { Text("Найти сервер") }, leadingIcon = { Icon(Icons.Outlined.Search, null) })
            if (state.settingsPending) Text("Сначала примените изменения подключения в настройках", color = colors.primary, fontSize = 12.sp)
            androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp), contentPadding = PaddingValues(bottom = 8.dp)) {
                if (visible.isEmpty()) item { Text("Сервер не найден", Modifier.padding(vertical = 24.dp), color = colors.onSurfaceVariant) }
                items(visible.size, key = { visible[it].tag }) { index ->
                    val server = visible[index]
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(if (server.selected) colors.primaryContainer.copy(.45f) else Color.Transparent)
                        .then(Modifier.testTag("quick-server:" + server.tag))
                        .clickable(enabled = !state.importing && !state.settingsPending && state.vpnStatus !in listOf(Status.Starting, Status.Stopping)) { select(server); dismiss() }
                        .heightIn(min = 64.dp).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(countryFlag(server.tag), fontSize = 23.sp)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(server.tag, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(server.type.uppercase(), fontSize = 10.sp, color = colors.onSurfaceVariant)
                        }
                        if (server.tag in state.favorites) Icon(Icons.Outlined.Star, null, Modifier.padding(end = 6.dp).size(16.dp), tint = colors.primary)
                        Text(server.delayMs?.let { "$it мс" } ?: "—", fontSize = 12.sp, color = colors.secondary)
                        if (server.selected) Icon(Icons.Outlined.Check, null, Modifier.padding(start = 6.dp).size(18.dp), tint = colors.primary)
                    }
                }
            }
            TextButton({ dismiss(); allRoutes() }, Modifier.fillMaxWidth().padding(bottom = 16.dp)) { Text("Все серверы и диагностика") }
        }
    }
}
