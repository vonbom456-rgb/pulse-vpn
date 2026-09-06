package app.pulse.vpn.data

import kotlinx.serialization.Serializable

/** A subscription can be saved without having a usable VPN configuration. */
@Serializable
data class SubscriptionIssue(val code: String, val message: String, val blocksConnection: Boolean = true) {
    val title: String get() = when (code) {
        "device_limit" -> "Лимит устройств"
        "device_id" -> "Устройство не принято"
        "access_denied" -> "Нет доступа к подписке"
        "expired" -> "Подписка закончилась"
        "network", "http", "format" -> "Не удалось обновить"
        "no_servers" -> "Серверы пока недоступны"
        else -> "Сообщение подписки"
    }
    val retryable: Boolean get() = code in setOf("network", "http", "format")
    val hint: String get() = when (code) {
        "device_limit" -> "Удалите ненужное устройство в кабинете или напишите в поддержку. Затем обновите подписку."
        "device_id" -> "Напишите в поддержку подписки и затем повторите обновление."
        "expired", "access_denied" -> "Проверьте подписку в кабинете или напишите в поддержку."
        else -> "Проверьте интернет и попробуйте обновить подписку позже."
    }

    companion object {
        fun isServiceEntry(tag: String, address: String): Boolean {
            val text = tag.lowercase()
            return text.startsWith("❌") || address.startsWith("error.", true) ||
                listOf("отсутствуют данные", "missing device", "device limit", "hwid", "лимит устройств",
                    "max devices", "too many devices", "слишком много устройств", "subscription expired", "подписка истекла", "подписка истёкла", "подписка закончилась",
                    "подписка отключена", "подписка неактивна", "подписка не активна", "subscription disabled", "subscription inactive", "traffic limit", "лимит трафика исчерпан").any(text::contains)
        }
        fun fromServiceMessages(messages: List<String>, blocking: Boolean): SubscriptionIssue {
            val message = messages.mapNotNull { SubscriptionMetadata.text(it, 500) }
                .map { it.replace(Regex("(?i)(?:https?|vless|vmess|trojan|ss|hysteria2|hy2|tuic)://\\S+"), "[ссылка]") }
                .distinct().take(4).joinToString("\n").ifBlank { "Подписка не выдала серверы для подключения." }
            val text = message.lowercase()
            val code = when {
                "лимит" in text && "устройств" in text || "device limit" in text || "max devices" in text || "too many devices" in text || "слишком много устройств" in text -> "device_limit"
                "hwid" in text || "missing device" in text -> "device_id"
                "истек" in text || "истёк" in text || "законч" in text || "expired" in text -> "expired"
                "отключена" in text || "неактивна" in text || "не активна" in text || "disabled" in text || "inactive" in text || "traffic limit" in text || "лимит трафика" in text -> "access_denied"
                else -> "service_message"
            }
            return SubscriptionIssue(code, message, blocking)
        }
    }
}
