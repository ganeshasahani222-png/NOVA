# Nova — AI Voice Assistant (Android)

Nova is a starter Android project for a voice + text AI assistant, built with
Kotlin and Jetpack Compose. It's structured so each capability — voice input,
AI responses, system intents, device admin actions — lives in its own module
behind a small interface, so you can extend or swap any one of them without
touching the others.

## Requirements

- Android Studio Koala (2024.1) or newer
- JDK 17
- Android SDK Platform 34
- A physical device or emulator running API 26+ (Android 8.0+)

## Getting started

1. Open the project root in Android Studio (`File > Open`, select the `Nova` folder).
2. Let Gradle sync (it will download the wrapper on first sync).
3. Run the `app` configuration on a device/emulator.

The app runs out of the box with a **stub AI engine** (`StubAiEngine`) that
echoes input back — no API key required — so you can see the full chat/voice
loop working immediately.

## Project structure

```
app/src/main/java/com/nova/assistant/
├── MainActivity.kt          # Compose entry point, runtime permission handling
├── NovaApplication.kt       # Holds the app-wide NovaContainer
├── core/
│   ├── NovaContainer.kt              # Manual DI composition root
│   └── NovaAccessibilityService.kt   # Placeholder/docs for future accessibility module
├── data/
│   └── ChatMessage.kt        # Framework-independent chat message model
├── ai/
│   ├── AiEngine.kt           # Interface all AI backends implement
│   ├── StubAiEngine.kt       # Offline placeholder implementation (default)
│   └── RemoteAiEngine.kt     # Skeleton for wiring up a real LLM API
├── voice/
│   ├── VoiceListener.kt              # App-specific callback contract
│   ├── SpeechRecognitionController.kt # Wraps Android SpeechRecognizer
│   └── WakeWordDetector.kt           # Interface placeholder for "Hey Nova" wake word
├── intents/
│   └── SystemActionDispatcher.kt     # Single audit point for Intents Nova can fire
├── admin/
│   ├── NovaDeviceAdminReceiver.kt    # Official android.app.admin.DeviceAdminReceiver
│   └── DeviceAdminManager.kt         # Thin wrapper over DevicePolicyManager
└── ui/
    ├── theme/                # Compose Material3 theme
    ├── components/           # MessageBubble, MicButton
    └── chat/                 # ChatScreen, ChatViewModel, factory
```

## Extending Nova

**Real AI responses.** Implement `AiEngine` (or fill in `RemoteAiEngine`'s
`buildRequestBody`/`parseResponseText` to match your chosen provider's API),
then swap `StubAiEngine` for it in `NovaContainer`. Load your API key from
the Android Keystore / EncryptedSharedPreferences or a backend you control —
never hardcode it in source.

**Wake-word detection.** Implement `WakeWordDetector` using a library like
Porcupine or Vosk. It should run inside a foreground `Service` (Android
requires a visible, ongoing notification for background microphone use) and
call `SpeechRecognitionController.startListening()` once triggered.

**More system actions.** Add methods to `SystemActionDispatcher` using
standard, documented `Intent` actions. Keeping all of them in one file makes
it easy to review exactly what Nova can trigger on the device.

**Accessibility features.** See the docstring in
`core/NovaAccessibilityService.kt` for the steps required — it's left
unregistered by default since `AccessibilityService` is a sensitive,
high-privilege API that should only be added for a concrete, disclosed
feature.

## About the Device Administrator integration

Nova includes a working `DeviceAdminReceiver` (`admin/NovaDeviceAdminReceiver`)
and a `DeviceAdminManager` wrapper around Android's official
[Device Admin API](https://developer.android.com/guide/topics/admin/device-admin).

A few things worth being explicit about:

- **Nova does not have, and cannot be extended to have, unrestricted system
  control.** Device admin apps can only use the specific policies they
  declare in `res/xml/device_admin_policies.xml` — currently `limit-password`,
  `watch-login`, and `force-lock` (screen lock on request). Any policy not
  listed there is simply unavailable to the app; this is enforced by Android
  itself, not by application logic.
- **Activation is always user-initiated.** `DeviceAdminManager.buildRequestAdminIntent()`
  builds the system prompt; the user must explicitly approve it in Settings.
  The app can never enable device admin silently.
- **The user can revoke it at any time** in Settings > Security > Device Admin
  Apps, or by calling `DeviceAdminManager.removeAdmin()` from within the app.
- **Play Store policy note:** Google restricts publishing consumer apps that
  use the Device Admin API to a narrow set of approved use cases, and
  generally steers enterprise/parental-control style features toward the
  Android Enterprise Device Policy Controller (DPC) APIs instead. If you plan
  to publish Nova with device-admin features, review current Google Play
  policy for the Device Admin API before submitting.

## Permissions used

| Permission | Why |
|---|---|
| `RECORD_AUDIO` | Voice input via `SpeechRecognizer` |
| `INTERNET`, `ACCESS_NETWORK_STATE` | Calling an AI API |
| `BIND_DEVICE_ADMIN` | Required to register the DeviceAdminReceiver; scope limited by `device_admin_policies.xml` |

No other permissions are requested. If you add features that need more
(camera capture, contacts, location, etc.), request them at the point of use
with the standard Android runtime permission APIs, and explain to the user
why before asking.

## License

This starter project is provided as-is for you to build on; add whatever
license fits your use case.
