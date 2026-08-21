---
name: Android build environment
description: The local workspace may have Gradle without an Android SDK.
---

Android projects can configure Gradle successfully but still fail at compile time when `ANDROID_HOME`/`sdk.dir` is absent.

**Why:** Installing Gradle alone does not provide Android platform/build-tool packages, and this workspace did not have an Android SDK available.

**How to apply:** Treat Gradle configuration success and APK compilation as separate checks; use a CI/Android Studio environment with a configured SDK for the final APK build.