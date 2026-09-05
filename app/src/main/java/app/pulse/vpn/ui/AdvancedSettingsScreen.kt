package app.pulse.vpn.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pulse.vpn.PulseUiState
import app.pulse.vpn.PulseViewModel
import app.pulse.vpn.Screen
import app.pulse.vpn.core.OptionCatalog
import app.pulse.vpn.core.OptionSpec
import app.pulse.vpn.core.AdvancedOptions

internal val LocalAdvancedOptions = staticCompositionLocalOf { AdvancedOptions() }

@Composable
internal fun AdvancedSettingsScreen(state: PulseUiState, viewModel: PulseViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    var selected by remember { mutableStateOf<OptionSpec?>(null) }
    var confirm by remember { mutableStateOf<String?>(null) }
    val filtered = OptionCatalog.all.filter { ("${it.title} ${it.hint} ${it.group}").contains(query, true) }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.navigate(Screen.SETTINGS) }) { Icon(Icons.Outlined.ArrowBack, "Назад") }
            Text("Все параметры", style = MaterialTheme.typography.headlineSmall)
        }
        OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            label = { Text("Поиск настроек") }, leadingIcon = { Icon(Icons.Outlined.Search, null) },
            trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, "Очистить поиск") } })
        if (state.settingsPending) {
            Text("Есть изменения для следующего подключения", modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.primary)
            Button(onClick = { viewModel.reconnect() }, modifier = Modifier.fillMaxWidth()) { Text("Применить и переподключить") }
        }
        filtered.groupBy { it.group }.forEach { (group, options) ->
            Text(group, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 22.dp, bottom = 8.dp))
            Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface) {
                Column {
                    options.forEachIndexed { index, spec ->
                        val current = state.options.text(spec.key)
                        val row = Modifier.fillMaxWidth()
                        Row(
                            (if (spec.toggle) row.toggleable(current == "true", role = Role.Switch) { viewModel.setOption(spec.key, it.toString()) }
                            else row.clickable { selected = spec }).padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(spec.title, style = MaterialTheme.typography.titleSmall)
                                Text(spec.hint, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                                if (!spec.toggle) Text(spec.choices.first { it.first == current }.second, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            if (spec.toggle) Switch(current == "true", onCheckedChange = null)
                            else Icon(Icons.Outlined.ChevronRight, null)
                        }
                        if (index != options.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
        if (filtered.isEmpty()) Text("Ничего не найдено. Попробуйте «DNS» или «экран».", Modifier.padding(vertical = 24.dp))
        if (query.isBlank()) {
            TextButton(onClick = { confirm = "history" }) { Text("Очистить историю текущей подписки") }
            TextButton(onClick = { confirm = "reset" }) { Text("Сбросить дополнительные параметры") }
        }
    }
    selected?.let { spec ->
        AlertDialog(onDismissRequest = { selected = null }, title = { Text(spec.title) },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) {
                spec.choices.forEach { (value, label) ->
                    Row(Modifier.fillMaxWidth().clickable { viewModel.setOption(spec.key, value); selected = null }.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(state.options.text(spec.key) == value, onClick = null)
                        Text(label, Modifier.weight(1f))
                    }
                }
            } }, confirmButton = { TextButton(onClick = { selected = null }) { Text("Закрыть") } })
    }
    confirm?.let { action ->
        AlertDialog(onDismissRequest = { confirm = null }, title = { Text(if (action == "history") "Очистить историю?" else "Сбросить параметры?") },
            text = { Text(if (action == "history") "Будут удалены только сохранённые замеры выбранной подписки." else "Дополнительные параметры вернутся к значениям по умолчанию. Подписки и списки приложений останутся.") },
            confirmButton = { TextButton(onClick = { if (action == "history") viewModel.clearPingHistory() else viewModel.resetAdvanced(); confirm = null }) { Text("Подтвердить") } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Отмена") } })
    }
}
