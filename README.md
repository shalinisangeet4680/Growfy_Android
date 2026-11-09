This is a Kotlin Multiplatform project targeting Android, iOS.

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
    folder is the appropriate location.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDevDebug` (local backend) or `:androidApp:assembleProdRelease`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :shared:testAndroidHostTest`
- iOS tests: `./gradlew :shared:iosSimulatorArm64Test`

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
### Running against a local backend (Android dev flavor)

The dev flavor calls `http://localhost:4000/api/v1/`. The backend lives in
[`facebook_automation/backend`](https://github.com/aashutosh-rana/facebook_automation); run it in mock mode
(`GBP_MOCK=true`, `META_MOCK=true`, `AI_MOCK=true`, `OTP_TEST_NUMBERS=8434230999:963101` in its `.env`).

```bash
cd ../facebook_automation/backend && npm run db:migrate && npm run start:dev   # API on :4000
adb reverse tcp:4000 tcp:4000                                                  # device localhost:4000 → Mac
./gradlew :androidApp:installDevDebug
```

Sign in with `8434230999`, code `963101`. On this machine Postgres runs on port **5433**
(something else owns 5432), so the backend's `DATABASE_URL` points at `localhost:5433`.
Re-run `adb reverse` after the emulator restarts.

Endpoints the app calls that the backend doesn't have yet are listed in [docs/backend-gaps.md](docs/backend-gaps.md).
