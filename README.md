# ExpenseTracker

Android expense tracking app built with Jetpack Compose, Room, Hilt, and Material 3.

## Prerequisites

- **JDK 17** or newer
- **Android SDK** with API level 35 (Android 15)
- Android Studio Ladybug (2024.2) or newer (optional, for IDE builds)

## Building the APK

### Debug APK

```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release APK (unsigned)

```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release-unsigned.apk`

### Signed release APK

1. Create a keystore (one-time):

   ```bash
   keytool -genkey -v -keystore release.keystore -alias mykey -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Create `keystore.properties` in the project root (do **not** commit this file):

   ```properties
   storeFile=../release.keystore
   storePassword=your_store_password
   keyAlias=mykey
   keyPassword=your_key_password
   ```

3. Add a signing config to `app/build.gradle.kts` inside the `android` block:

   ```kotlin
   signingConfigs {
       create("release") {
           val props = java.util.Properties().apply {
               load(rootProject.file("keystore.properties").inputStream())
           }
           storeFile = file(props["storeFile"] as String)
           storePassword = props["storePassword"] as String
           keyAlias = props["keyAlias"] as String
           keyPassword = props["keyPassword"] as String
       }
   }
   ```

   Then set `signingConfig = signingConfigs.getByName("release")` in the `release` build type.

4. Build:

   ```bash
   ./gradlew assembleRelease
   ```

## Installing on a device

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or directly build and install:

```bash
./gradlew installDebug
```

## Running tests

```bash
./gradlew test              # Unit tests
./gradlew connectedCheck    # Instrumented tests (requires device/emulator)
```
