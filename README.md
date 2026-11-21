# AION Control — Foundation 0.5.0

Kotlin + Jetpack Compose Android foundation for a permission-based device companion.

## Added in 0.5.0
- Local allowlisted command interpreter in `commands/AionCommandEngine.kt`.
- Supported commands: open Android settings, display settings, sound settings, and launch an installed app by its exact visible label.
- Two-step flow: analyze command, inspect the proposed action, then explicitly confirm execution.
- Unknown commands are rejected. No arbitrary shell, script, hidden screen capture, or background UI control is exposed.

## Existing foundation
- Device metrics: RAM, storage, battery, Android version and device model.
- Installed launchable app listing and app launch.
- Android settings shortcuts and media volume control.
- Accessibility actions: Back, Home, Recents, click visible text, type into focused field, and scroll. These require the user to enable AION Accessibility in Android settings.

## Setup
Open this folder in Android Studio, allow Gradle sync, then run on an Android device (minSdk 26).

## Important limitations
- This archive has not been compiled or tested on a physical Android device in this environment.
- The ChatGPT bridge, relay server, Shizuku/root integration, MediaProjection capture, voice engine, and emergency privacy workflow are not implemented yet.
- Android permissions and user confirmation remain required. Accessibility actions operate only after the user enables the service.
- The local command interpreter is intentionally narrow; it is not a general-purpose natural-language agent or terminal.
