package app.pulse.vpn.data

import kotlinx.serialization.Serializable

@Serializable
data class VpnProfile(
    val id: String,
    val name: String,
    val sourceUrl: String? = null,
    val updatedAt: Long,
    val uploadBytes: Long? = null,
    val downloadBytes: Long? = null,
    val totalBytes: Long? = null,
    val expireAt: Long? = null,
    /** Optional provider hint. It is never used as a routing/configuration field. */
    val themeHint: String? = null,
)

data class VpnServer(
    val tag: String,
    val type: String,
    val address: String?,
    val port: Int?,
    val selected: Boolean,
    val delayMs: Int? = null,
)

/** Provider metadata outbound (description/contact), never a selectable route. */
fun VpnServer.isInfoMetadata(): Boolean =
    tag.contains("info", ignoreCase = true) || address?.contains("info.", ignoreCase = true) == true

data class ImportedProfile(
    val name: String,
    val config: String,
    val sourceUrl: String?,
    val userInfo: SubscriptionUserInfo,
    val themeHint: String? = null,
)

data class SubscriptionUserInfo(
    val upload: Long? = null,
    val download: Long? = null,
    val total: Long? = null,
    val expire: Long? = null,
)

sealed interface ImportResult {
    data class Success(val profile: VpnProfile) : ImportResult
    data class Error(val message: String) : ImportResult
}
