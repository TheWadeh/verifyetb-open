# Contributing to Verifyetb

Thanks for your interest in contributing! Verifyetb is an offline Android expense
tracker for Ethiopian banks and mobile wallets.

## Getting started

1. Fork the repo and clone your fork.
2. Open the project in Android Studio (JDK 17 required).
3. Build & test:
   ```bash
   ./gradlew :app:testDebugUnitTest
   ./gradlew :app:assembleDebug
   ```

## Guidelines

- Keep changes focused and small; one feature/fix per PR.
- Follow existing Kotlin + Jetpack Compose style.
- Add/update tests for parser changes (see `SmsParserPipelineTest`).
- Don't commit build artifacts, APKs, or `local.properties`.

## Adding a new bank/wallet

Parsers live in `app/src/main/java/com/verifyetb/app/parser/`. Add a parser
following the existing pattern and register the institution in
`EthiopianInstitutions.kt`. Include sample (redacted) SMS in tests.

## Pull requests

Open a PR against `main`. Describe what changed and how you tested it.
