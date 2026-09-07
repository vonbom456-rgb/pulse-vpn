package app.pulse.vpn.data

import org.junit.Assert.*
import org.junit.Test

class SubscriptionUsageTest {
    private val profile = VpnProfile("test", "Test", updatedAt = 0)
    @Test fun absentCountersAreUnknownNotUnlimited() { assertNull(SubscriptionUsage.from(profile, 0).limit); assertNull(SubscriptionUsage.from(profile, 0).daysLeft) }
    @Test fun addsBothDirections() { val usage = SubscriptionUsage.from(profile.copy(uploadBytes = 20, downloadBytes = 30, totalBytes = 100), 0); assertEquals(50L, usage.remaining); assertEquals(.5f, usage.fraction!!, 0f) }
    @Test fun ignoresNegativeCounters() { assertEquals(12L, SubscriptionUsage.from(profile.copy(uploadBytes = -2, downloadBytes = 12), 0).used) }
    @Test fun saturatesOverflow() { val usage = SubscriptionUsage.from(profile.copy(uploadBytes = Long.MAX_VALUE, downloadBytes = 12, totalBytes = 100), 0); assertEquals(Long.MAX_VALUE, usage.used); assertEquals(0L, usage.remaining); assertTrue(usage.exhausted) }
    @Test fun roundsRemainingPartialDayUp() { assertEquals(1L, SubscriptionUsage.from(profile.copy(expireAt = 1001), 1_000_000).daysLeft) }
    @Test fun expiredIsZeroDays() { assertEquals(0L, SubscriptionUsage.from(profile.copy(expireAt = 999), 1_000_000).daysLeft) }
    @Test fun millisecondsMatchSeconds() { assertEquals(SubscriptionUsage.from(profile.copy(expireAt = 1_900_000_000), 0), SubscriptionUsage.from(profile.copy(expireAt = 1_900_000_000_000), 0)) }
    @Test fun zeroQuotaHasNoProgress() { assertNull(SubscriptionUsage.from(profile.copy(totalBytes = 0), 0).fraction) }
}
