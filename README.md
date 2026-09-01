# Dreaming Spanish TV

Minimal Android TV wrapper for:

`https://app.dreaming.com/spanish/browse?sort=easy&hide-watched=true`

The app opens the site fullscreen in a native `WebView`, keeps cookies for login, supports Android TV launcher metadata, forwards remote-control select events to the focused page element, and handles fullscreen HTML5 video.

## Remote control

- DPAD left/right/up/down selects the nearest video, menu item, or button in that direction.
- The on-screen pointer follows the selected element.
- DPAD up/down scrolls the page when there is no selectable item further in that direction.
- OK/Enter clicks the selected element.
- Channel up/down or page up/down scrolls a full page.
- Back exits fullscreen video, then navigates back, then exits the app.

## Build

Open this folder in Android Studio, or build from the command line after Gradle is working:

```sh
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
ANDROID_HOME="$HOME/Library/Android/sdk" \
./gradlew assembleDebug
```

## Install

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

On first launch, sign in to Dreaming Spanish with the TV remote or a connected keyboard. Login cookies are persisted by WebView.
