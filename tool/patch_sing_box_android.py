from pathlib import Path
import os


pub_cache = Path(os.environ.get("PUB_CACHE", Path.home() / ".pub-cache"))
matches = list(
    pub_cache.glob(
        "hosted/*/flutter_sing_box-1.1.5/android/src/main/kotlin/"
        "com/clashsing/flutter_sing_box/FlutterSingBoxPlugin.kt"
    )
)
if len(matches) != 1:
    raise SystemExit(f"Expected one flutter_sing_box Kotlin file, found {len(matches)}")

plugin_file = matches[0]
source = plugin_file.read_text()

replacements = {
    "import android.content.Intent\n": (
        "import android.app.Activity\n"
        "import android.content.Intent\n"
        "import java.lang.ref.WeakReference\n"
    ),
    "        private const val VPN_REQUEST_CODE = 1001\n": (
        "        private const val VPN_REQUEST_CODE = 1001\n"
        "        private var hostActivity: WeakReference<Activity>? = null\n\n"
        "        @JvmStatic\n"
        "        fun attachHostActivity(activity: Activity) {\n"
        "            hostActivity = WeakReference(activity)\n"
        "        }\n\n"
        "        @JvmStatic\n"
        "        fun detachHostActivity(activity: Activity) {\n"
        "            if (hostActivity?.get() === activity) hostActivity = null\n"
        "        }\n"
    ),
    "    private val pendingStartVpnResult = AtomicReference<Result?>(null)\n": (
        "    private val pendingStartVpnResult = AtomicReference<Result?>(null)\n\n"
        "    private fun currentActivity(): Activity? =\n"
        "        activityBinding?.activity ?: hostActivity?.get()\n"
    ),
    "val activity = activityBinding?.activity": "val activity = currentActivity()",
}

for old, new in replacements.items():
    if old not in source:
        raise SystemExit(f"flutter_sing_box patch anchor not found: {old!r}")
    source = source.replace(old, new)

plugin_file.write_text(source)
print(f"Patched {plugin_file}")
