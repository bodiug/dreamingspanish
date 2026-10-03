# Dreaming Spanish TV

This is an unofficial, independent community project. It is not affiliated with,
endorsed by, or sponsored by Dreaming Spanish or Dreaming. All third-party names
and trademarks belong to their respective owners. Using this app requires your
own access to the Dreaming Spanish service.

Dreaming Spanish TV is a small Android TV app that opens the Dreaming Spanish web app in a fullscreen native `WebView`.

It starts at:

```text
https://app.dreaming.com/spanish/browse?sort=easy&hide-watched=true
```

The app is built for TV remote navigation. It forces a desktop-style viewport, keeps the left menu visible, makes video cards easier to select, hides distracting watch-page sections, and adds TV-friendly controls for fullscreen playback, pause/play, quality selection, refresh, autoplay settings, and session reset.

This app was built with AI assistance.

The source code is available under the [MIT License](LICENSE). This license does
not grant rights to Dreaming Spanish's content, branding, or service.

## Features

- Android TV launcher support with app icon and banner.
- Fullscreen WebView for the Dreaming Spanish website.
- Persistent Dreaming Spanish login through WebView cookies/storage.
- Network loading without the WebView HTTP cache; login and settings remain stored.
- Desktop viewport on TV to avoid the mobile layout.
- Remote-friendly selection navigation for video cards, toolbar filters, menus, and watch-page controls.
- Optional mouse-pointer navigation mode.
- Watch-page cleanup that focuses on the video and description.
- Support for both Dreaming Spanish/Shaka videos and embedded YouTube videos.
- TV fullscreen mode for video playback.
- Website autoplay toggle.
- Auto fullscreen toggle.
- Page refresh and session reset from the in-app settings menu.

## Remote Control

Default navigation mode is selection-based:

- `DPAD left/right/up/down`: move the current selection.
- `OK` / `Enter`: activate the selected item.
- `Back`: close an open menu, go back, or exit fullscreen.
- Long press `Back`: open the app settings menu.
- `Channel up/down`, `Page up/down`, or media seek keys: scroll the page.
- Media play/pause keys: toggle video playback when supported by the current player.

On browse pages, the app prefers selecting video cards and avoids most surrounding buttons. On watch pages, the selectable player controls are limited to play/pause, quality, and fullscreen where possible.

## Settings Menu

Open the settings menu with a long press on `Back`.

Available settings:

- `Navigation`: switch between selection mode and mouse-pointer mode.
- `Website autoplay`: enable or disable Dreaming Spanish autoplay for the next video.
- `Auto fullscreen`: enable or disable automatic TV fullscreen after selecting a video.
- `Refresh page`: reload the current Dreaming Spanish page.
- `Reset session`: clear WebView cookies, cache, local storage, session storage, history, and reload the start page.

## Build Requirements

- Android Studio or a local Android SDK.
- JDK compatible with the Android Gradle Plugin.
- Android SDK platform for `compileSdk 37`.
- ADB if you want to install from the command line.

This project uses the Gradle wrapper included in the repository.

## Debug Build

From the repository root:

```sh
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
ANDROID_HOME="$HOME/Library/Android/sdk" \
./gradlew assembleDebug
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Install On Android TV

Connect to the device with ADB, then install the debug APK:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

For a network-connected Android TV device:

```sh
adb connect <device-ip>:5555
adb -s <device-ip>:5555 install -r app/build/outputs/apk/debug/app-debug.apk
```

Start the app from the Android TV launcher, or from ADB:

```sh
adb shell monkey -p com.bodiug.dreamingspanish -c android.intent.category.LEANBACK_LAUNCHER 1
```

## Clean Build Output

```sh
./gradlew clean
```

Generated Gradle files, APKs, local SDK paths, and build outputs are ignored by Git.

## Release Builds

Only debug signing is configured at the moment. For a release APK or Android App Bundle, add a release signing config in `app/build.gradle`, keep signing secrets out of Git, and then run the relevant Gradle task:

```sh
./gradlew assembleRelease
```

or:

```sh
./gradlew bundleRelease
```

## Continuous Integration and Releases

Every push and pull request runs `.github/workflows/ci.yml`, which builds the
debug APK to catch build breakage early.

Pushing a version tag (`vX.Y.Z`) runs `.github/workflows/release.yml`, which
builds the debug APK, appends the commits since the previous tag to
[`CHANGELOG.md`](CHANGELOG.md), and publishes a GitHub Release with that APK
attached:

```sh
git tag v1.0.0
git push origin v1.0.0
```

## Development Notes

`MainActivity.java` is the orchestrator: Activity lifecycle, `WebView` setup, and remote key handling. The other TV behavior lives in dedicated classes:

- `TvWebScripts.java`: loads and caches the injected JS, and builds the small calls into it.
- `SettingsOverlay.java`: the in-app settings menu UI and its options.
- `DesktopHtmlInterceptor.java`: rewrites the initial HTML response to force the desktop viewport.
- `CursorView.java`: the on-screen pointer used in mouse mode.

The CSS and JavaScript injected into Dreaming Spanish pages after load — to adapt layout, navigation, playback controls, fullscreen behavior, autoplay handling, and menu focus for Android TV — lives in `app/src/main/assets/js/`:

- `ds-tv-core.js`: layout CSS, the shared player-controls overlay, and D-pad spatial navigation.
- `ds-tv-focus.js`: focus styling and initial tab order.
- `ds-tv-autoplay.js`: the website autoplay toggle.
