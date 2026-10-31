# Changelog

## v1.0.2

### Added

- Add an Exit command to stop playback and close the app from settings.
- Add regression tests and Android Lint checks to continuous integration.

### Fixed

- Apply website autoplay settings in clean sessions and when opening another video.
- Honor the automatic fullscreen setting and cancel outdated fullscreen requests.
- Prevent previous player events from prematurely closing fullscreen.
- Support player controls in both mouse and selection navigation modes.
- Hide selected player controls after inactivity and limit Shaka quality-selection retries.
- Return to the start page instead of browser history when pressing Back outside fullscreen.
- Restore website focus after closing settings and support modern Android Back handling.
- Load fresh video data instead of initially showing a persisted, outdated video list.

### Improved

- Cache static website assets while keeping HTML and website data fresh.
- Reuse layout measurements within each remote-key action.
- Batch relevant player updates and avoid repeated autoplay scans and storage writes.
- Start automatic fullscreen when the player is ready instead of using a fixed startup delay.
- Simplify settings by using Back to close the menu and remove duplicate fullscreen code.

## v1.0.1

- Prepare v1.0.1 and fix Android SDK setup (b3fc82b)
- Add MIT license and clarify unofficial project status (c9e2d47)
- Fix frozen time display when revealing player controls remotely (6828af5)
- Exit TV fullscreen automatically when the video ends (890f1c7)
- Update changelog for v1.0.0 (042dbdf)

## v1.0.0

- Stop hardcoding a machine-specific org.gradle.java.home (2952189)
- Fix CI: correct Android platform package id to android-37.0 (97fac12)
- Fix CI: use android-actions/setup-android to expose sdkmanager (7bb0fb6)
- Add CI build check and tag-triggered release pipeline (0ac8efe)
- Update README development notes for the class/asset split (5c3e2b6)
- Fix player controls staying visible after player node swap (57d1713)
- Enable WebView remote debugging in debug builds (4455a52)
- Extract inline JS to asset files and split MainActivity (7dab54e)
- Handle fullscreen back before exit (16fb579)
- Unify TV player controls (22e67d7)
- Document app usage and builds (69de274)
- Initial Android TV WebView app (8128d1e)
