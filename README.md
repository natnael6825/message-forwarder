# Message Forwarder

Relay is an open-source Android SMS forwarding app built with React Native, Expo, and a small local Kotlin module. It watches for new SMS messages from selected phone numbers, short codes, or company sender IDs and sends cleaned copies to one or more recipients.

Relay is designed for personal Android installations. It has no server, account system, analytics, or cloud message store.

## Features

- Create and manage multiple forwarding configurations.
- Enable or pause each configuration from the main screen.
- Select a sender from recent SMS messages, or enter a number, short code, or company name manually.
- Select recipients from Android contacts and add multiple destination numbers.
- Clean sensitive text before forwarding by entering a full example such as `balance is ETB 234`. Relay generates a variable-number pattern that also matches `balance is 40`.
- Run forwarding in the background through an Android SMS receiver.
- Show a persistent notification while at least one configuration is enabled.
- Keep a local history of forwarding attempts and view the cleaned message that was sent.
- Reassemble multipart SMS messages before applying rules.
- Reject duplicate broadcasts and Relay-to-Relay loops.
- Use a dark interface with a bottom tab menu that respects Android navigation insets.

## Important Android limitations

This app uses the sensitive `RECEIVE_SMS`, `READ_SMS`, and `SEND_SMS` permissions. Android and Google Play place additional restrictions on SMS apps. Review Google's [SMS permissions policy](https://support.google.com/googleplay/android-developer/answer/10208820) before distributing the app through Google Play.

The app is intended for a phone you own or administer. SMS forwarding can expose private messages and may create carrier charges. Test with numbers and messages you control. Forwarding means that Android handed the message to the carrier; it does not confirm delivery to the recipient.

Relay does not support MMS, RCS, WhatsApp, iMessage, old inbox imports, or cloud synchronization. Android force-stop, battery optimization, manufacturer restrictions, revoked permissions, or a missing SIM can prevent background forwarding.

## Requirements

- Node.js 20 or newer.
- Android Studio, Android SDK, and an Android device or emulator.
- A JDK supported by the installed Expo/Android Gradle versions. JDK 21 is used for the tested local build.
- USB debugging enabled when installing to a physical device.

Expo Go is not supported because Expo Go cannot load this custom native SMS module. Use a development build or a standalone APK.

## Run the development build

```powershell
git clone https://github.com/natnael6825/message-forwarder.git
cd message-forwarder
npm install
npx expo run:android
```

After the native development build is installed, `npm start` starts the Metro server for that build. Approve SMS, notification, and contacts permissions when the app requests them.

## Build a standalone APK

With an Expo account, create an internal APK through EAS:

```powershell
npx eas-cli build --platform android --profile preview
```

To build locally:

```powershell
npx expo prebuild --platform android
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-21'
$env:ANDROID_HOME = "$env:LOCALAPPDATA/Android/Sdk"
cd android
.\gradlew.bat :app:assembleRelease --no-daemon
```

The APK is written to `android/app/build/outputs/apk/release/app-release.apk`. Expo prebuild creates the `android` directory locally; it is intentionally ignored by Git. For distribution, configure your own signing credentials instead of using a development signing key.

On Windows, React Native's C++ build can exceed the path-length limit in a deeply nested OneDrive checkout. If that happens, copy the project to a short path such as `C:\relay-build` before running the Gradle build. Keep source edits in the original checkout.

## Use Relay

1. Open **Create** and give the configuration a name.
2. Under **Forward from**, choose a received SMS or enter the sender exactly as Android shows it. Company sender IDs and short codes are supported.
3. Under **Send to**, choose contacts or enter full destination numbers. Add as many recipients as needed.
4. Optional: under **Clean up message**, enter the words as they appear in a message. For example, entering `balance is ETB 234` makes Relay remove both `balance is ETB 234` and `balance is 40` before sending.
5. Save the configuration, return to **Routes**, and enable its switch.
6. Review **History** to see sent, failed, or pending attempts and open the cleaned message.

Numeric senders are normalized by removing formatting, but local and international forms are still treated as different values. Company names match the complete sender ID case-insensitively. A contact's display name is not necessarily the sender ID used by the SMS network.

## Project structure

| Path | Purpose |
| --- | --- |
| `App.tsx` | Relay screens, configuration editor, pickers, permissions, history, and cleanup-example conversion. |
| `modules/sms-forwarder/index.ts` | TypeScript bridge and shared configuration types. |
| `modules/sms-forwarder/android` | Kotlin Expo module, SMS receiver, persistence, notification, boot handling, and native tests. |
| `app.json` | Expo identity, Android package name, icons, permissions, and backup settings. |
| `eas.json` | Development, preview APK, and production build profiles. |
| `assets` | Relay application icons. |

The Kotlin module stores configurations and the last 50 forwarding results in private app storage. Android backup is disabled. No message data is sent to a Relay server.

## Verify changes

```powershell
npm run typecheck
npx expo-doctor
npx expo prebuild --platform android
cd android
.\gradlew.bat :sms-forwarder:testDebugUnitTest
```

Native tests cover sender matching, company IDs, short codes, paused rules, loop prevention, and cleanup examples. Real SMS delivery still needs a SIM-equipped phone and test numbers you control.

## Contributing

Issues and pull requests are welcome. Please include the Android version, device model, permission state, and relevant log output when reporting a forwarding problem. Never include real message contents, phone numbers, or private logs in a public issue.

## License

This project is released under the [MIT License](LICENSE).
