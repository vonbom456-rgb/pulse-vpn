package app.pulse.vpn.core

import kotlinx.serialization.Serializable

data class OptionSpec(
    val key: String, val title: String, val hint: String, val group: String,
    val default: String, val choices: List<Pair<String, String>> = emptyList(),
    val reconnect: Boolean = false,
) {
    val toggle get() = choices.isEmpty()
    fun accepts(value: String) = if (toggle) value in listOf("true", "false") else choices.any { it.first == value }
}

/** One validated catalog drives storage, UI, and effective connection settings. */
object OptionCatalog {
    private fun choice(key: String, title: String, hint: String, group: String, default: String, vararg values: Pair<String, String>, reconnect: Boolean = false) =
        OptionSpec(key, title, hint, group, default, values.toList(), reconnect)
    private fun toggle(key: String, title: String, hint: String, group: String, default: Boolean = false, reconnect: Boolean = false) =
        OptionSpec(key, title, hint, group, default.toString(), reconnect = reconnect)
    val all = listOf(
        choice("mtu", "Размер пакета MTU", "Меньше — полезно при зависании сайтов. «Из профиля» оставляет значение из подписки.", "Туннель", "0", "0" to "Из профиля", "1280" to "1280", "1400" to "1400", "1500" to "1500", reconnect = true),
        choice("stack", "Сетевой стек", "Меняйте при проблемах совместимости с сетью.", "Туннель", "profile", "profile" to "Из профиля", "system" to "System", "gvisor" to "gVisor", "mixed" to "Mixed", reconnect = true),
        toggle("bypass_lan", "Локальная сеть напрямую", "Принтеры и частные IP-адреса обходят VPN. Не включайте в недоверенной сети.", "Туннель", reconnect = true),
        toggle("block_quic", "Отключить QUIC", "Блокирует UDP/443: браузер сможет перейти на TCP. Может мешать некоторым приложениям.", "Туннель", reconnect = true),
        toggle("metered", "Лимитное VPN-соединение", "Android сообщит приложениям, что трафик нужно экономить (Android 10+).", "Туннель", reconnect = true),
        choice("connect_timeout", "Ожидание подключения", "Если сервер не ответит за это время, подключение остановится. Можно попробовать снова.", "Туннель", "30", "15" to "15 с", "30" to "30 с", "60" to "60 с"),
        choice("dns_strategy", "Семейство DNS-адресов", "Только IPv4/IPv6 ограничивает ответы DNS, но не является блокировкой IP-трафика.", "DNS", "profile", "profile" to "Из профиля", "prefer_ipv4" to "IPv4 первым", "prefer_ipv6" to "IPv6 первым", "ipv4_only" to "Только IPv4", "ipv6_only" to "Только IPv6", reconnect = true),
        toggle("dns_cache", "Кэш DNS", "Повторные запросы используют кэш до истечения TTL.", "DNS", true, true),
        toggle("dns_independent", "Раздельный кэш DNS", "Не смешивать ответы разных DNS-серверов.", "DNS", true, true),
        choice("ping_timeout", "Ожидание ответа сервера", "Ожидание ответа одного адреса. Проверка без VPN не измеряет скорость туннеля.", "Проверка серверов", "1200", "800" to "0,8 с", "1200" to "1,2 с", "2500" to "2,5 с", "5000" to "5 с"),
        choice("ping_parallel", "Параллельные проверки", "Меньше — ниже нагрузка на сеть и устройство.", "Проверка серверов", "6", "2" to "2", "4" to "4", "6" to "6", "10" to "10"),
        toggle("ping_retry", "Повтор при неудаче", "Ещё одна TCP-попытка, если ни один IP не ответил.", "Проверка серверов", true),
        choice("slow_ms", "Порог медленного сервера", "Используется фильтром на экране маршрутов.", "Проверка серверов", "250", "150" to "150 мс", "250" to "250 мс", "500" to "500 мс"),
        toggle("save_history", "Сохранять историю пингов", "При выключении результаты остаются только до закрытия приложения.", "Проверка серверов", true),
        choice("server_sort", "Порядок серверов", "Выбранный порядок применяется на экране маршрутов.", "Список серверов", "profile", "profile" to "Из профиля", "name" to "По имени", "ping" to "По задержке", "favorites" to "Избранные выше"),
        toggle("hide_addresses", "Скрыть адреса серверов", "Убирает адрес и порт из списков и карточки подробностей.", "Приватность"),
        toggle("secure_screen", "Запретить скриншоты", "Запрещает скриншоты и скрывает приложение в недавних. Может отключить трансляцию экрана.", "Приватность"),
        toggle("haptics", "Отклик кнопки подключения", "Короткая вибрация при нажатии.", "Интерфейс", true),
        toggle("ui_animations", "Анимации интерфейса", "Плавные переходы, раскрытие карточек и подсветка вкладок.", "Интерфейс", true),
        toggle("keep_screen", "Не гасить экран при VPN", "Только пока приложение открыто и VPN подключён. Увеличивает расход батареи.", "Интерфейс"),
        toggle("compact_card", "Сворачивать подписку", "При открытии показывать только название и статус. Нажмите стрелку, чтобы увидеть подробности и серверы.", "Интерфейс"),
        toggle("show_home_servers", "Серверы на главной", "Показывает до трёх серверов под карточкой подписки.", "Интерфейс", true),
        toggle("notification_speed", "Скорость в уведомлении", "Обновлять скорость в уведомлении активного VPN.", "Интерфейс", true, true),
        toggle("wifi_refresh", "Обновлять только по Wi-Fi", "Ограничивает ручные и автоматические запросы обновления подписок.", "Обновление подписок"),
        choice("refresh_hours", "Частота автообновления", "Минимальный интервал автообновления при открытии. Ручное обновление доступно всегда.", "Обновление подписок", "0", "0" to "Всегда", "1" to "1 час", "6" to "6 часов", "24" to "Сутки"),
    )
    val byKey = all.associateBy { it.key }
}

@Serializable
data class AdvancedOptions(val values: Map<String, String> = emptyMap()) {
    fun text(key: String): String {
        val spec = requireNotNull(OptionCatalog.byKey[key])
        return values[key]?.takeIf(spec::accepts) ?: spec.default
    }
    fun bool(key: String) = text(key) == "true"
    fun number(key: String) = text(key).toInt()
    fun with(key: String, value: String): AdvancedOptions {
        val spec = requireNotNull(OptionCatalog.byKey[key])
        require(spec.accepts(value))
        return copy(values = values + (key to value))
    }
}
