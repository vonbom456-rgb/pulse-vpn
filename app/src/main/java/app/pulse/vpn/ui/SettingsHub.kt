package app.pulse.vpn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pulse.vpn.PulseUiState
import app.pulse.vpn.PulseViewModel
import app.pulse.vpn.Screen
import app.pulse.vpn.core.OptionCatalog
import io.nekohasekai.sfa.constant.Status

private data class SettingsEntry(
    val key: String, val title: String, val hint: String, val group: String,
    val value: String = "", val choices: List<Pair<String, String>> = emptyList(),
    val toggle: Boolean = false, val enabled: Boolean = true,
    val change: (String) -> Unit = {}, val action: (() -> Unit)? = null,
)
private data class SettingsSection(val title: String, val hint: String, val icon: ImageVector)
private val sections = listOf(
    SettingsSection("Подключение", "Режим VPN и приложения", Icons.Outlined.Route),
    SettingsSection("DNS и сеть", "DNS-сервер, кэш и адреса", Icons.Outlined.Language),
    SettingsSection("Серверы и пинг", "Выбор маршрута и проверка", Icons.Outlined.Speed),
    SettingsSection("Подписки", "Профили и обновления", Icons.Outlined.Devices),
    SettingsSection("Оформление", "Темы, анимация и главный экран", Icons.Outlined.Palette),
    SettingsSection("Приватность", "Адреса и защита экрана", Icons.Outlined.Lock),
    SettingsSection("Дополнительно", "Туннель, уведомления и ресурсы", Icons.Outlined.Tune),
)

private fun entries(state: PulseUiState, vm: PulseViewModel, openVpnSettings: () -> Unit): List<SettingsEntry> {
    val base = listOf(
        SettingsEntry("routing", "Маршрутизация", "Что направлять через VPN", "Подключение", state.routingMode,
            listOf("rules" to "По правилам подписки", "global" to "Весь трафик", "direct" to "Напрямую, без VPN"), change = { vm.setRoutingMode(it) }),
        SettingsEntry("apps", "Приложения", when (state.perAppMode) { 1 -> "Только выбранные · ${state.selectedApps.size}"; 2 -> "Исключения · ${state.selectedApps.size}"; else -> "Все приложения" },
            "Подключение", action = { vm.navigate(Screen.APPS) }),
        SettingsEntry("auto_connect", "Автоподключение", "Запуск после перезагрузки устройства", "Подключение", state.autoConnect.toString(), toggle = true, change = { vm.setAutoConnect(it.toBoolean()) }),
        SettingsEntry("always_on", "Защита при обрыве", "Постоянный VPN и блокировка трафика в настройках Android", "Подключение", action = openVpnSettings),
        SettingsEntry("dns", "DNS-сервер", "Изменение применяется при следующем подключении", "DNS и сеть", state.dnsMode,
            listOf("local" to "Из профиля", "cloudflare" to "Cloudflare", "google" to "Google", "quad9" to "Quad9", "adguard" to "AdGuard DNS"), change = { vm.setDnsMode(it) }),
        SettingsEntry("auto_fastest", "Выбирать быстрый сервер", "Автоматический выбор после проверки задержки", "Серверы и пинг", state.autoFastest.toString(), toggle = true, change = { vm.setAutoFastest(it.toBoolean()) }),
        SettingsEntry("routes", "Список серверов", "Избранные, протоколы и подробности", "Серверы и пинг", action = { vm.navigate(Screen.ROUTES) }),
        SettingsEntry("stats", "Статистика сессии", "Скорость и переданный трафик", "Серверы и пинг", action = { vm.navigate(Screen.STATS) }),
        SettingsEntry("profiles", "Мои подписки", "${state.profiles.size} профилей", "Подписки", action = { vm.navigate(Screen.PROFILES) }),
        SettingsEntry("refresh_on_open", "Обновлять при открытии", "Обновлять подписку при запуске приложения", "Подписки", state.refreshOnOpen.toString(), toggle = true, change = { vm.setRefreshOnOpen(it.toBoolean()) }),
        SettingsEntry("refresh", "Обновить подписки", if (state.importing) "Обновляем…" else "Обновить серверы, срок и остаток трафика", "Подписки",
            enabled = !state.importing && state.profiles.any { it.sourceUrl != null }, action = { vm.refreshSubscriptions() }),
        SettingsEntry("theme", "Цветовая тема", "Фон, текст, карточки и кнопки", "Оформление", state.accentTheme,
            listOf("pulse" to "Pulse", "ocean" to "Ocean", "ember" to "Ember", "midnight" to "Midnight", "mono" to "Mono", "profile" to "Из подписки"), change = vm::setAccentTheme),
        SettingsEntry("dark", "Тёмное оформление", "Тёмные цвета на всех экранах", "Оформление", state.darkTheme.toString(), toggle = true, change = { vm.setDarkTheme(it.toBoolean()) }),
        SettingsEntry("effects", "Живой фон", "Мягкое свечение и анимация подключения", "Оформление", state.liveEffects.toString(), toggle = true, change = { vm.setLiveEffects(it.toBoolean()) }),
    )
    return base + OptionCatalog.all.map { spec ->
        val section = when {
            spec.key == "connect_timeout" -> "Подключение"
            spec.group == "DNS" -> "DNS и сеть"
            spec.group in listOf("Проверка серверов", "Список серверов") -> "Серверы и пинг"
            spec.group == "Обновление подписок" -> "Подписки"
            spec.group == "Приватность" -> "Приватность"
            spec.group == "Интерфейс" && spec.key !in listOf("notification_speed", "keep_screen") -> "Оформление"
            else -> "Дополнительно"
        }
        SettingsEntry(spec.key, spec.title, spec.hint, section, state.options.text(spec.key), spec.choices, spec.toggle, change = { vm.setOption(spec.key, it) })
    }
}

@Composable
internal fun SettingsHub(state: PulseUiState, viewModel: PulseViewModel, openVpnSettings: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var choiceKey by rememberSaveable { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf<String?>(null) }
    var help by remember { mutableStateOf<SettingsEntry?>(null) }
    val all = entries(state, viewModel, openVpnSettings)
    val matches = all.filter { ("${it.title} ${it.hint} ${it.group} " + it.choices.joinToString { option -> option.second }).contains(query.trim(), true) }
    val colors = MaterialTheme.colorScheme
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().imePadding(),
        contentPadding = PaddingValues(18.dp, 12.dp, 18.dp, 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Настройки", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Подключение и внешний вид", fontSize = 12.sp, color = colors.onSurfaceVariant)
                }
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(colors.primaryContainer), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Tune, null, tint = colors.onPrimaryContainer)
                }
            }
        }
        item {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(18.dp),
                label = { Text("Поиск настроек") }, leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = { if (query.isNotBlank()) IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, "Очистить поиск") } })
        }
        if (query.isBlank()) item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("routing" to "РЕЖИМ VPN", "dns" to "DNS").forEach { (key, label) ->
                    val entry = all.first { it.key == key }
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(listOf(colors.primaryContainer.copy(.65f), colors.surface)))
                        .border(1.dp, colors.primary.copy(.17f), RoundedCornerShape(18.dp))
                        .clickable { choiceKey = key }.padding(14.dp)) {
                        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                        Text(entry.choices.firstOrNull { it.first == entry.value }?.second ?: "Выбрать", fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 5.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("DNS", "Пинг", "Цветовая тема", "MTU", "Приватность").forEach { shortcut ->
                    SuggestionChip(onClick = { query = shortcut }, label = { Text(shortcut, fontSize = 12.sp) })
                }
            }
            }
        }
        if (state.settingsPending) item {
            Surface(color = colors.primaryContainer, shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("Нужно переподключиться", fontWeight = FontWeight.SemiBold)
                    Text("Настройки сохранены. Переподключитесь, чтобы они заработали.", fontSize = 12.sp)
                    Button(onClick = { viewModel.reconnect() }, enabled = state.vpnStatus !in listOf(Status.Starting, Status.Stopping), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Переподключиться") }
                }
            }
        }
        items(sections.filter { section -> matches.any { it.group == section.title } }, key = { it.title }) { section ->
            val groupEntries = matches.filter { it.group == section.title }
            val open = query.isNotBlank() || section.title in expanded
            Surface(shape = RoundedCornerShape(24.dp), color = colors.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (open) colors.primary.copy(.27f) else colors.outlineVariant.copy(.55f))) {
                Column {
                    Row(Modifier.fillMaxWidth().clickable {
                        expanded = if (section.title in expanded) expanded - section.title else expanded + section.title
                    }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(colors.primary.copy(.12f)), contentAlignment = Alignment.Center) {
                            Icon(section.icon, null, Modifier.size(21.dp), tint = colors.primary)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(section.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text(if (query.isBlank()) section.hint else "Найдено: ${groupEntries.size}", fontSize = 11.sp, color = colors.onSurfaceVariant)
                        }
                        Text(groupEntries.size.toString(), color = colors.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
                        Icon(if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, if (open) "Свернуть ${section.title}" else "Развернуть ${section.title}", Modifier.size(20.dp))
                    }
                    PulseDisclosure(open) {
                        Column {
                            groupEntries.forEach { entry ->
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = colors.outlineVariant.copy(.45f))
                                if (entry.key == "theme") {
                                    Text(entry.title, Modifier.padding(start = 16.dp, top = 12.dp), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        entry.choices.forEach { (value, label) ->
                                            PulseTheme(state.darkTheme, if (value == "profile") state.selectedProfile?.themeHint ?: "pulse" else value) {
                                                val palette = MaterialTheme.colorScheme
                                                Column(Modifier.width(112.dp).clip(RoundedCornerShape(16.dp))
                                                    .background(Brush.linearGradient(listOf(palette.background, palette.primaryContainer)))
                                                    .border(if (entry.value == value) 2.dp else 1.dp, if (entry.value == value) palette.primary else palette.outlineVariant, RoundedCornerShape(16.dp))
                                                    .clickable { entry.change(value) }.padding(12.dp)) {
                                                    Icon(if (entry.value == value) Icons.Outlined.Check else Icons.Outlined.Palette, null, tint = palette.primary, modifier = Modifier.size(24.dp))
                                                    Text(label, color = palette.onSurface, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp), maxLines = 2)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    val row = Modifier.fillMaxWidth()
                                    Row((if (entry.toggle) row.toggleable(entry.value == "true", enabled = entry.enabled, role = Role.Switch) { entry.change(it.toString()) }
                                    else row.clickable(enabled = entry.enabled) { if (entry.action != null) entry.action.invoke() else choiceKey = entry.key })
                                        .padding(start = 16.dp, end = 12.dp, top = 13.dp, bottom = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(entry.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.onSurface.copy(if (entry.enabled) 1f else .45f))
                                            Text(entry.choices.firstOrNull { it.first == entry.value }?.second ?: entry.hint,
                                                color = if (entry.choices.isNotEmpty()) colors.primary else colors.onSurfaceVariant,
                                                fontSize = 11.sp, lineHeight = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(top = 3.dp))
                                        }
                                        if (entry.toggle) Switch(entry.value == "true", onCheckedChange = null, enabled = entry.enabled, modifier = Modifier.padding(start = 8.dp))
                                        else Icon(Icons.Outlined.ChevronRight, null, Modifier.padding(start = 8.dp).size(20.dp), tint = colors.onSurfaceVariant)
                                        if (entry.key in OptionCatalog.byKey) IconButton(onClick = { help = entry }, modifier = Modifier.size(48.dp)) { Icon(Icons.Outlined.Info, "Подробнее: ${entry.title}", modifier = Modifier.size(18.dp), tint = colors.onSurfaceVariant) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (matches.isEmpty()) item {
            Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.Search, null, tint = colors.onSurfaceVariant)
                Text("Настройка не найдена", Modifier.padding(top = 8.dp), fontWeight = FontWeight.SemiBold)
                Text("Попробуйте «DNS», «экран» или «пинг».", color = colors.onSurfaceVariant, fontSize = 12.sp)
                TextButton(onClick = { query = "" }) { Text("Показать все разделы") }
            }
        }
        if (query.isBlank()) item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { confirm = "history" }) { Text("Очистить замеры", fontSize = 12.sp) }
                TextButton(onClick = { confirm = "reset" }) { Text("Сброс параметров", fontSize = 12.sp) }
            }
            Text("Pulse VPN ${app.pulse.vpn.BuildConfig.VERSION_NAME} · ${viewModel.coreVersion()}", color = colors.onSurfaceVariant, fontSize = 10.sp, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        }
    }
    all.firstOrNull { it.key == choiceKey }?.let { entry ->
        AlertDialog(onDismissRequest = { choiceKey = null }, title = { Text(entry.title) },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(entry.hint, fontSize = 13.sp, color = colors.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                entry.choices.forEach { (value, label) ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { entry.change(value); choiceKey = null }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(entry.value == value, onClick = null)
                        Text(label, Modifier.weight(1f))
                    }
                }
            } }, confirmButton = { TextButton(onClick = { choiceKey = null }) { Text("Закрыть") } })
    }
    help?.let { entry -> AlertDialog(onDismissRequest = { help = null }, title = { Text(entry.title) }, text = { Text(entry.hint) },
        confirmButton = { TextButton(onClick = { help = null }) { Text("Понятно") } }) }
    confirm?.let { action ->
        AlertDialog(onDismissRequest = { confirm = null }, title = { Text(if (action == "history") "Очистить историю?" else "Сбросить дополнительные параметры?") },
            text = { Text(if (action == "history") "Удалятся замеры текущей подписки. Серверы и настройки останутся." else "Дополнительные настройки вернутся к исходным, включая анимации и приватность. Подписки, темы и списки приложений останутся.") },
            confirmButton = { TextButton(onClick = { if (action == "history") viewModel.clearPingHistory() else viewModel.resetAdvanced(); confirm = null }) { Text(if (action == "history") "Очистить" else "Сбросить") } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Отмена") } })
    }
}
