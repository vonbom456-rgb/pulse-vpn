package app.pulse.vpn

import androidx.test.platform.app.InstrumentationRegistry
import app.pulse.vpn.core.ProfileManager
import app.pulse.vpn.data.*
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Synthetic HTTP replies only; never contacts a real subscription service. */
class UnavailableSubscriptionPersistenceTest {
    @Test fun saveRecoverKeepCachedAndBlockDeniedRefresh() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var stage = 0
        val importer = SubscriptionImporter(client = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(when (stage) { 2 -> 503; 3 -> 403; else -> 200 }).message("Fixture")
                .apply { if (stage == 0) header("x-hwid-max-devices-reached", "true") }
                .header("profile-title", "Pulse Persistence Fixture")
                .header("support-url", "https://t.me/pulse_demo")
                .body(if (stage == 1) """{"outbounds":[{"type":"vless","tag":"Test","server":"example.invalid","server_port":443,"uuid":"11111111-1111-4111-8111-111111111111"}]}""".toResponseBody() else "".toResponseBody())
                .build()
        }.build())
        val repository = ProfileRepository(context, importer)
        val previous = repository.profiles().firstOrNull { it.id == repository.selectedId() }
        var created: VpnProfile? = null
        try {
            val added = repository.importProfile("https://example.invalid/sub/persistence") as ImportResult.Success
            created = added.profile
            val saved = ProfileRepository(context).profiles().first { it.id == added.profile.id }
            assertEquals("device_limit", saved.issue?.code)
            assertTrue(repository.servers(saved).isEmpty())
            assertNull(ProfileManager.getSelectedProfile())
            repository.applyRoutingSettings(saved)
            assertNull(ProfileManager.getSelectedProfile())
            assertTrue(File(context.filesDir, "profiles/${saved.id}/using_config.json").readText().contains("reject"))

            stage = 1
            val recovered = (repository.update(saved) as ImportResult.Success).profile
            assertNull(recovered.issue)
            assertEquals(1, repository.servers(recovered).size)
            assertNotNull(ProfileManager.getSelectedProfile())
            val runtime = ProfileManager.getUsingConfig().readText()

            stage = 2
            val cached = (repository.update(recovered) as ImportResult.Success).profile
            assertEquals("http", cached.issue?.code)
            assertFalse(cached.issue!!.blocksConnection)
            assertEquals(recovered.updatedAt, cached.updatedAt)
            assertEquals(1, repository.servers(cached).size)
            assertEquals(runtime, ProfileManager.getUsingConfig().readText())

            stage = 3
            val denied = (repository.update(cached) as ImportResult.Success).profile
            assertEquals("access_denied", denied.issue?.code)
            assertTrue(repository.servers(denied).isEmpty())
            assertNull(ProfileManager.getSelectedProfile())
            val reloaded = ProfileRepository(context).profiles().first { it.id == saved.id }
            assertEquals(denied.issue, reloaded.issue)
            assertEquals(recovered.providerSupportUrl, reloaded.providerSupportUrl)
        } finally {
            created?.let { repository.delete(it) }
            previous?.let { repository.applyRoutingSettings(it); repository.select(it) }
        }
    }
}

