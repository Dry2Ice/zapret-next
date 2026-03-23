# Android module bootstrap

Android module is now configured as a standalone Gradle project (`android/`) with module `:app`.

## Requirements

- JDK 17+
- Android SDK (API 35 platform)
- Gradle available in `PATH`

## Gradle CLI

Use lightweight CLI wrapper from repository root:

```bash
./android/gradle-cli tasks
./android/gradle-cli :app:testDebugUnitTest
./android/gradle-cli :app:assembleDebug -- --stacktrace
```

The wrapper forwards tasks to:

```bash
gradle -p ./android <task>
```
