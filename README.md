# Tap Share

Tap Share is a starter Android app for contact exchange over proximity workflows.

## Important Android limitation

Modern Android versions no longer support silent phone-to-phone NFC contact transfer. Android Beam, the old peer-to-peer NFC feature, was removed from Android 10. A production app cannot make two locked phones exchange contacts simply by being near each other with no user action.

This prototype therefore does the closest platform-compliant flow:

- Keep Tap Share open on the receiving phone.
- Bring it near an NFC source that publishes a `text/vcard` NDEF record.
- Tap Share parses the vCard and opens Android's contact import screen.

For a production-quality "nearby" experience, use NFC as a tap trigger and then hand off to a consent-based channel such as Nearby Connections, Bluetooth LE, Wi-Fi Aware, or a share link/QR code.

## Project layout

- `app/src/main/java/com/example/tapshare/MainActivity.java` contains the prototype UI, vCard generation, NFC foreground dispatch, and contact-import logic.
- `app/src/main/AndroidManifest.xml` declares NFC support and the vCard NDEF intent filter.

## Build

Install Android Studio or the Android Gradle Plugin command-line dependencies, then run:

```bash
gradle assembleDebug
```
