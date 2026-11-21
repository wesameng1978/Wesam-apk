# Wissam File Master — Updated Implementation Notes

## Fixed in this update
- Quick Access tiles now navigate to real folders/categories: Apps, Download, DCIM, Movies and Documents.
- Settings is now an in-app screen rather than opening phone Settings; it includes appearance/privacy information and a language selector with 20 languages.
- Apps top-right menu now has sorting, refresh and visible-app count.
- Every app row has a working overflow menu: Launch, Android App Info, Extract/Share APK, and package action.
- Files now browses real shared external storage, supports folder navigation/back navigation, and opens files through Android intents/FileProvider.
- Tools cards now have explicit in-app behavior/messages instead of dead buttons or a phone Settings redirect.

## Build status
`./gradlew clean assembleDebug bundleRelease` completed successfully. The debug APK and release AAB are generated under `app/build/outputs/`.

## Important Android limitations
- APK extraction is subject to Android package visibility and filesystem access. For split APK applications, this version shares the accessible package source and does not claim a complete split-package backup.
- The release AAB is unsigned; configure your own upload key before Play submission.
- The language menu contains 20 selectable languages. Full translation of every string into all 20 locales is a follow-up localization pass; the selector and RTL-ready Arabic resource foundation are included.
- Storage browsing follows Android's shared-storage rules and cannot promise protected app-private paths.

## Performance hotfix — v1.0.1
- File discovery now runs on `Dispatchers.IO` with cancellation of stale scans.
- Installed-app discovery now runs off the main thread, is guarded against duplicate reloads, and is capped to a safe visible list size.
- File listings are capped at 250 entries per directory to avoid freezing on huge folders.
- APK/app refresh is explicit and shows a loading indicator instead of blocking the screen.
- Folder rows no longer call `listFiles()` during Compose rendering.
