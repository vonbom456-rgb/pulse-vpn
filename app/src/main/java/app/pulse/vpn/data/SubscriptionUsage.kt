package app.pulse.vpn.data

/** Provider counters can be absent, negative, or too large to add safely. */
internal data class SubscriptionUsage(val used: Long, val limit: Long?, val daysLeft: Long?) {
    val remaining: Long? get() = limit?.let { (it - used).coerceAtLeast(0) }
    val fraction: Float? get() = limit?.let { (used.toDouble() / it).coerceIn(0.0, 1.0).toFloat() }
    val exhausted: Boolean get() = limit != null && used >= limit

    companion object {
        fun from(profile: VpnProfile, nowMillis: Long = System.currentTimeMillis()): SubscriptionUsage {
            val upload = (profile.uploadBytes ?: 0).coerceAtLeast(0)
            val download = (profile.downloadBytes ?: 0).coerceAtLeast(0)
            val used = if (Long.MAX_VALUE - upload < download) Long.MAX_VALUE else upload + download
            val expires = profile.expireAt?.takeIf { it > 0 }?.let { if (it > 100_000_000_000L) it / 1000 else it }
            val seconds = expires?.let { (it - nowMillis.coerceAtLeast(0) / 1000).coerceAtLeast(0) }
            return SubscriptionUsage(used, profile.totalBytes?.takeIf { it > 0 }, seconds?.let { it / 86400 + if (it % 86400 > 0) 1 else 0 })
        }
    }
}
