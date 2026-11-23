# Verifyetb

> Offline Android expense tracker for Ethiopian banks and mobile wallets.
> No account. No server. Your data stays on your phone.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](#)
[![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-blue.svg)](#)

## Screenshot

![Verifyetb screenshot](docs/screenshot.png)

## What it does

Verifyetb reads the transaction SMS your Ethiopian bank or mobile wallet
already sends to your phone and turns them into a clean, searchable expense
log — completely offline.

Supported institutions (via SMS):

- **Banks:** CBE, Awash, Dashen, BOA, Abyssinia, Zemen, Wegagen, Nib,
  Amhara, Oromia, Berhan, Hibret, Siiqee, Hijra, Ahadu, Abay, ZamZam, …
- **Wallets:** Telebirr, M-Pesa, CBE Birr, eBirr, Amole, and more.

## Features

- 📱 **Offline-first** — no account, no cloud, no tracking.
- 💬 **SMS auto-import** — parses debit/credit/fee/airtime messages.
- 💰 **Balances & accounts** — per-institution balances with manual overrides.
- 🔍 **Search & filters** — by institution, type, period, fees-only.
- 📝 **Notes** — attach notes to any transaction.
- 📊 **Insights** — monthly summaries and spending breakdowns.
- 🌍 **Amharic-friendly** — built for the Ethiopian market.

## Screens

1. Onboarding / SMS permission
2. Dashboard — balance, month summary, recent list, needs-note prompt
3. Transactions — search + filters
4. Transaction detail / add note
5. Add manually
6. Settings — SMS scan, reprocess, categories, full institution list

## Build

Requirements: JDK 17, Android SDK (compileSdk 35).

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Release builds require a `keystore.properties` file (see `app/build.gradle.kts`).

## Contributing

Contributions are welcome! See [CONTRIBUTING.md](CONTRIBUTING.md).

Adding a new bank/wallet? Parsers live in
`app/src/main/java/com/verifyetb/app/parser/` — follow the existing pattern and
register the institution in `EthiopianInstitutions.kt`.

## License

[MIT](LICENSE) © TheWadeh
# verifyetb-open
