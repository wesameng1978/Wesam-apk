# Wissam File Master

Updated native Android Studio project using Kotlin, Jetpack Compose Material 3, ViewModel and Gradle.

## Open
Open the `WissamFileMaster` folder in Android Studio. The project uses Gradle Wrapper 8.11.1 and compile/target SDK 35.

## Build
```bash
./gradlew clean assembleDebug bundleRelease
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
Release AAB: `app/build/outputs/bundle/release/app-release.aab`

The AAB is a release build without a private signing key. Configure your own keystore in Android Studio/Gradle for Play publication.

## Updated behavior
Quick Access navigates into real categories; Files browses and opens accessible storage; Apps menus launch, open Android App Info, and extract/share package APKs where Android permits; Settings and Tools are in-app screens; the language selector offers 20 languages.

## Privacy
The app is offline-first and does not request Internet permission. File and package data are read locally and shown only on the device.

## GitHub collaboration and automatic builds

Every push runs `.github/workflows/android-build.yml`. Open the repository's **Actions** tab, select **Android Build**, and download the APK/AAB from the completed workflow artifacts. The release AAB is unsigned until a repository signing secret and release signing configuration are added.
