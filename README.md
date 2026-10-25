# Noir Lens — Android camera and offline photo editor (v1)

Noir Lens is a dark, pro-inspired Android photography app built with Kotlin, Jetpack Compose, and CameraX. There is no expense tracker.

## Version 1 features

- CameraX live viewfinder and still capture saved to `Pictures/NoirLens`
- Rule-of-thirds grid toggle
- Flash toggle
- Zoom slider (CameraX zoom ratio, capped at 4x in this UI)
- Exposure compensation cycling from -2 EV to +2 EV when supported by the device
- Photo picker and offline color studio
- Brightness, contrast, saturation, and warmth adjustments
- Save a graded copy without overwriting the original
- Dark interface; core camera and editing features do not require internet

## Honest limitations in v1

- AI enhancement, object removal, and generative editing are not implemented in this first source drop. The AI button explains this. An AI model/service needs to be integrated and tested before these can be claimed as working.
- The current PRO tab is an informational toggle; manual ISO, shutter speed, focus distance, RAW capture, lens switching, and histogram are not yet implemented. Actual supported camera capabilities differ by phone. Exposure compensation and zoom are wired to CameraX.
- The editor's live preview uses a Compose color matrix; Save applies brightness/contrast/saturation/warmth to a copy and writes it as JPEG. This is a simple starter editor, not a non-destructive professional RAW pipeline.
- The project has not been compiled or tested on a physical Android device in this environment. Build it in Android Studio and resolve any toolchain/device-specific issues before distributing.

## Architecture

- `MainActivity.kt`: Compose UI, CameraX preview/capture, camera controls, editor and local export.
- `CameraPreview`: binds CameraX preview and `ImageCapture` to the activity lifecycle.
- `EditorScreen`: offline image adjustment UI.
- `gradeBitmap`: creates a graded copy for export.
- `saveCapture`: saves camera output through MediaStore.

## Build an APK with Android Studio

1. Install the latest stable Android Studio and a JDK 17 (Android Studio's bundled JBR is suitable).
2. Extract this ZIP.
3. In Android Studio, choose **Open** and select the extracted `NoirLens` folder (the folder containing `settings.gradle.kts`).
4. Allow Gradle sync. Ensure Android SDK Platform 35 and a compatible Android Gradle Plugin are installed. Gradle needs internet access the first time to download dependencies.
5. Choose **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
6. Android Studio will show a notification with **Locate**. The debug APK is normally at `app/build/outputs/apk/debug/app-debug.apk`.
7. Transfer that APK to your Android phone and install it. If Android asks, allow installation from that file manager/source. Only install builds you trust.

## Build from terminal

After opening once in Android Studio, you can use its configured Gradle wrapper if generated, or from the project directory run `gradle assembleDebug` where a compatible Gradle installation is available. Output: `app/build/outputs/apk/debug/app-debug.apk`.

## Package details

- App label: Noir Lens
- Application ID: `com.noirlens.camera`
- Minimum Android version: Android 8.0 (API 26)
- Target SDK: 35
- Language: Kotlin
- UI: Jetpack Compose Material 3
- Camera: CameraX

## Next recommended milestones

1. Add a real settings screen and camera lens selector.
2. Expose device-supported ISO, shutter speed, focus distance, white balance, RAW/JPEG and capture resolution using Camera2 interop/capability checks.
3. Add a dedicated gallery screen and EXIF details.
4. Add curated LUT presets and stronger color-grading controls.
5. Integrate on-device AI using a compatible model, with optional user-consented online providers separately.
6. Add tests and validate on multiple camera devices before a release build.

## Build from an Android phone (no computer)

See [PHONE-ONLY-BUILD.md](PHONE-ONLY-BUILD.md). The included GitHub Actions workflow builds a debug APK in the cloud and publishes it as a downloadable artifact.
