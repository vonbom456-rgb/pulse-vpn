from pathlib import Path


manifest = Path("android/app/src/main/AndroidManifest.xml")
source = manifest.read_text()
marker = '            </intent-filter>\n'
deep_link = '''            </intent-filter>
            <intent-filter android:autoVerify="false">
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="pulsevpn" android:host="auth" />
            </intent-filter>
'''
if 'android:scheme="pulsevpn"' not in source:
    if marker not in source:
        raise SystemExit("Main activity intent-filter anchor not found")
    source = source.replace(marker, deep_link, 1)
manifest.write_text(source)
