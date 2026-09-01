# ULTRON — Native Android App (Phase 1)

## Recommended: build in the cloud with GitHub Actions (no PC installs needed)

This is the easiest path if you're on a shared/institute PC — you only need
a free GitHub account and a browser.

1. Go to https://github.com and create a free account if you don't have one.
2. Create a new repository (any name, e.g. "ultron-app") — keep it Private
   or Public, your choice.
3. On the repo page, click "Add file" → "Upload files", then drag in
   **everything inside this unzipped `UltronApp` folder**, including the
   hidden `.github` folder (if your file browser hides dot-folders, show
   hidden files first, or use GitHub Desktop / `git` instead of drag-drop).
4. Commit the upload.
5. Go to the "Actions" tab on your repo → you'll see "Build Ultron APK" →
   click "Run workflow" → "Run workflow" again to confirm.
6. Wait 2-5 minutes for it to finish (green checkmark = success).
7. Click into the finished run → under "Artifacts" → download
   `ultron-debug-apk` (it's a zip containing the `.apk`).
8. Transfer that `.apk` to your phone any way you like (email it to
   yourself, Google Drive, USB, whatever's easiest) and open it on your
   phone to install. Android will ask you to allow "install from this
   source" the first time — allow it.

No JDK, no Android SDK, no Gradle, nothing installed on the institute PC at
all. If step 6 shows a red X (build failed), click into it to see the error
log and paste it here — I'll fix the project code.

---

## Alternative: build locally (only if you get a PC you can install on)

This is a real Android app project (Kotlin), not a Termux script. It fixes
the two biggest walls we hit on Termux:
- **No more beep/tone** — uses `SpeechRecognizer` directly instead of the
  popup-based recognizer Termux relies on.
- **Real cross-app control (Phase 2)** — via an Accessibility Service,
  Android's sanctioned way to read/tap inside other apps.

## What's in Phase 1
- App skeleton (MainActivity with Start/Stop buttons + a shortcut to enable
  the Accessibility Service)
- A foreground service that listens continuously, detects wake/sleep
  phrases, auto-sleeps after 15s of inactivity, and speaks replies
- An Accessibility Service stub with `findNodeByText()` / `clickNode()`
  helpers — ready for Phase 2, not wired to voice commands yet

## What's NOT done yet (Phase 2/3)
- Opening specific apps, calling contacts (straightforward — same ideas as
  the Termux version, just using proper Android APIs instead of shelling
  out to `termux-*` commands)
- Actually using the Accessibility Service for cross-app actions
- The glowing HUD as the app's own screen instead of a browser tab

## How to build this (no Android Studio required)

You need:
1. **JDK 17** — install from https://adoptium.net (pick your OS)
2. **Android SDK command-line tools** — download "Command line tools only"
   from https://developer.android.com/studio#command-tools
   (yes, that page is Android Studio's page — scroll down for the
   command-line-only download, it's much lighter)

Setup (run once):
```
# unzip the command-line tools into a folder, e.g. ~/android-sdk/cmdline-tools/latest
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

Set environment variables (add to your shell profile):
```
export ANDROID_HOME=~/android-sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin
```

Build the APK — this project doesn't include the Gradle wrapper binary (it's
normally auto-downloaded, and I don't have network access here to fetch it),
so install Gradle directly instead:
- Download from https://gradle.org/releases (get 8.7 or newer)
- Or, if you'd rather use the wrapper: install Gradle once, then run
  `gradle wrapper` inside the `UltronApp` folder — that generates the
  missing wrapper files for you, and after that `./gradlew` works normally
  for every build going forward.

Then build:
```
cd UltronApp
gradle assembleDebug
```

The output APK lands at:
```
app/build/outputs/apk/debug/app-debug.apk
```

Install it on your phone:
```
adb install app/build/outputs/apk/debug/app-debug.apk
```
(Enable "USB debugging" in your phone's Developer Options first, and plug
it into your PC via USB.)

## First run
1. Open the Ultron app on your phone
2. Grant the microphone/phone/contacts permissions when prompted
3. Tap "Enable Accessibility Service" → find Ultron in the list → turn it on
   (Android will show a warning dialog about what this permission allows —
   that's normal and expected for any accessibility-based app)
4. Tap "Start Ultron Service"
5. Say "hello Ultron" and confirm it responds — no tone, no popup

## If Gradle build fails
This project skeleton hasn't been build-tested in a real Android SDK
environment yet (I don't have one available here to verify it compiles
cleanly). Likely first-run issues are usually version mismatches between
the Android Gradle Plugin, Kotlin plugin, and installed SDK/build-tools
versions. Paste the exact error here and I'll fix it — this is the normal
first step of getting any fresh Android project building, not a sign
something is fundamentally wrong.

## Next steps
Once this builds and the basic wake/sleep loop works on your phone, tell me
and we'll move to Phase 2: wiring in open-app / call-contact / Wikipedia
lookup (same ideas as the Termux version, cleaner APIs) and the first real
Accessibility Service actions.
