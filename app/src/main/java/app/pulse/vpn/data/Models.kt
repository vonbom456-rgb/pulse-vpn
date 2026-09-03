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
)

data class VpnServer(
    val tag: String,
    val type: String,
    val address: String?,
    val port: Int?,
    val selected: Boolean,
    val delayMs: Int? = null,
)

data class ImportedProfile(
    val name: String,
    val config: String,
    val sourceUrl: String?,
    val userInfo: SubscriptionUserInfo,
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
