# Crashlytics setup

BookFlow already includes Firebase Crashlytics for Kotlin/Java failures, ANRs and native NDK failures. The integration is intentionally inactive until Firebase supplies an Android configuration for this exact app.

## Firebase app

- Android package: `app.bookflow.reader`
- Downloaded configuration: `google-services.json`
- Local destination: `app/google-services.json` (ignored by Git)

When that file exists, Gradle automatically applies the Google Services and Crashlytics plugins. Without it, the same source builds normally and `CrashReporter` remains a safe no-op.

## GitHub Actions

Encode the complete `google-services.json` as base64 and store it in the repository secret:

`FIREBASE_GOOGLE_SERVICES_JSON_BASE64`

The Android workflow reconstructs the file without printing its contents. If the secret is absent, CI clearly reports that crash diagnostics are inactive and still creates the APK.

## Privacy and verification

BookFlow's non-fatal reporter sends only an allow-listed technical area, exception type, sanitized stack trace, app version/build and layout class. It does not add the book title, document URI, book text or a reader identity.

There is no deliberate crash button in production. After Firebase is connected, verify delivery with a controlled development-only failure on a test build and reopen the app so Crashlytics can upload the report.

Official references:

- https://firebase.google.com/docs/crashlytics/android/get-started
- https://firebase.google.com/docs/crashlytics/android/get-started-ndk
