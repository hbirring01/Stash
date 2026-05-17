# StashApp — Best Credit Card Nearby

Android app that tells you **which credit card to swipe at the business in front of you** for the highest rewards. Pulls your linked cards via Plaid, finds nearby businesses via Foursquare + OpenStreetMap, and ranks every visible place by the multiplier your cards earn there.

<p align="center">
  <img src="screenshots/map.png"      width="240" alt="Rewards map with AI best-card hero" />
  <img src="screenshots/wallet.png"   width="240" alt="Wallet / cards home" />
  <img src="screenshots/settings.png" width="240" alt="Settings · API keys" />
</p>

<p align="center">
  <a href="https://hbirring01.github.io/CreditCardApp/privacy.html"><b>Privacy policy</b></a> ·
  <a href="RELEASING.md">Release checklist</a> ·
  <a href="server/README.md">Plaid proxy server</a>
</p>

---

## What's new in v1.4.1

- 🧭 **Rewards and Offers split into dedicated bottom tabs** — the combined hub is now two focused destinations: a Rewards tab (map + best-card hero) and an Offers tab (issuer offers tracker). Cleaner navigation, faster access, and less context-switching when you're standing at the register.
- 🗺️ **Stabilized map & list scrolling** — refined gesture handling on the Rewards map and the Offers list so panning, pull-to-refresh, and button taps no longer fight each other.

## What's new in v1.4.0

- 🎯 **Card-linked offers tracker** — surfaces active issuer offers (Amex, Chase, etc.) you can manually add, see your savings progress on, and one-tap deep-link into the issuer app to activate.
- 🛎️ **Proximity notifications** — when you walk into a place that matches one of your unactivated offers, you get a notification with a tap-through to activate. Works both in the foreground (map open) and **in the background via system geofences** — fully opt-in with a clear two-step location permission flow.
- 🔁 **Boot recovery** — geofences are automatically re-installed after device reboot or app upgrade via a `BroadcastReceiver` + `HiltWorker`, so background offer alerts survive power cycles without needing you to reopen the app.
- 🤖 **AI Best-Card hero** on the map (v1.3) — picks the optimal card for the focused place using effective multiplier (base vs. quarterly rotating bonus), then breaks ties using sign-up bonus progress.
- 🔐 **Write-only API key management** in Settings (v1.3) — keys stored in `EncryptedSharedPreferences` backed by the Android Keystore.
- 📄 **Public privacy policy** at [hbirring01.github.io/CreditCardApp/privacy.html](https://hbirring01.github.io/CreditCardApp/privacy.html) — required for Plaid production access.

## Features

- 🗺️ **Rewards map** — Google-Maps-style tiles (CARTO Voyager) with colored markers by reward category (dining, gas, groceries, travel, shopping, entertainment).
- 💳 **Best-card recommendations** — every business is matched against your linked cards and ranked by per-dollar multiplier. Tap a card to see alternative options and expected points on a sample $50 spend.
- 📍 **Smart location** — auto-detect via GPS, or manually type a city / ZIP / address.
- 🔍 **Two search modes**
  - **Nearest match (in-list, ~30 mi)**: type a name in the lower search box; hits Foursquare's name-aware search and falls back to Overpass (OSM `name`/`brand`/`operator` regex) when no key is set.
  - **Anywhere (global, top bar)**: type `"Mezeh in Ellicott City"` or `"Best Buy near 90210"` — searches with Foursquare's `near=` parameter, no device location required.
- 🎯 **Category filter chips + sort toggle** — switch between sort-by-distance and sort-by-multiplier.
- 🔁 **Pull-to-refresh** + **"Search this area"** floating button when you pan the map.
- 🔒 **Encrypted local storage** — Room + SQLCipher for transaction cache, EncryptedSharedPreferences for Plaid tokens and Foursquare key, biometric/PIN-locked app entry.
- 🏦 **Plaid Link** — link real bank accounts in sandbox or production; transactions hydrate the card list and categorize spending.
- 📐 **Imperial units** throughout (feet up to ~⅒ mi, then miles).

<p align="center">
  <img src="screenshots/plaid.png" width="280" alt="Plaid Link setup" />
</p>

## Architecture

Single-module Android app, **MVVM + Hilt + Coroutines + Compose**.

```
app/
├── data/
│   ├── local/        Room DAOs, SQLCipher, migrations
│   ├── plaid/        Plaid API client + EncryptedSharedPreferences credentials store
│   ├── places/       Foursquare + Overpass repository
│   ├── preferences/  ApiKeyStore (write-only Foursquare key)
│   └── location/     FusedLocation + Geocoder
├── di/               Hilt modules (Network, Database, Plaid)
├── domain/           CreditCard, RewardCategory, multipliers
└── ui/
    ├── auth/         Biometric + PIN gate
    ├── home/         Dashboard
    ├── rewards/      Map screen (osmdroid + Compose) · BestCardHero
    ├── cards/        Add/edit cards
    ├── settings/     API key + Plaid management dialogs
    └── plaidsetup/   Plaid Link launcher
server/               Optional Node.js Plaid proxy (sandbox)
docs/                 Privacy policy + landing page (GitHub Pages)
```

| Layer        | Stack                                                                |
| ------------ | -------------------------------------------------------------------- |
| UI           | Jetpack Compose · Material 3 · Compose Navigation                    |
| State        | ViewModel + StateFlow                                                |
| DI           | Hilt 2.52                                                            |
| Persistence  | Room 2.6.1 · SQLCipher 4.5.4 · EncryptedSharedPreferences (Tink)     |
| Networking   | Retrofit 2.11.0 · OkHttp 4.12.0 · kotlinx-serialization 1.7.2        |
| Maps         | osmdroid 6.1.18 · CARTO Voyager raster tiles                         |
| Places       | Foursquare Places API (2025-06-17) · Overpass / OpenStreetMap        |
| Geocoding    | Android `Geocoder`                                                   |
| Banking      | Plaid Link Android SDK 4.6.0                                         |
| Security     | AndroidX Biometric 1.2 · BCrypt PIN hashing · Android Keystore       |
| Build        | AGP 8.5.2 · Kotlin 2.0.20 · KSP 2.0.20-1.0.25 · Gradle 8.7 · JDK 17  |

## Getting started

### Prerequisites

- Android Studio Koala+ (AGP 8.5.2)
- JDK 17
- Android SDK platform 34, build-tools 34.x
- Physical device or emulator running Android 8.0 (API 26) or higher

### Clone & configure

```bash
git clone https://github.com/hbirring01/CreditCardApp.git
cd CreditCardApp
```

Create `local.properties` (already git-ignored) with at minimum:

```properties
sdk.dir=C:\\Users\\<you>\\AppData\\Local\\Android\\Sdk

# Optional — bakes a default Foursquare key into the build. Users can override
# it at runtime under Settings → Foursquare API key (stored encrypted).
# Get a free key at https://foursquare.com/developers/
FOURSQUARE_API_KEY=fsq3YOUR_KEY_HERE

# Optional — release signing
# RELEASE_STORE_FILE=/path/to/keystore.jks
# RELEASE_STORE_PASSWORD=…
# RELEASE_KEY_ALIAS=…
# RELEASE_KEY_PASSWORD=…
```

### Build & run

```bash
./gradlew assembleDebug
./gradlew installDebug
```

Or open the project in Android Studio and hit ▶.

### Plaid setup

Tap **Set up Plaid** on first launch (or **Settings → Plaid keys** later) and paste:

- **Client ID** + **Secret** from https://dashboard.plaid.com/developers/keys
- Choose `sandbox` to use Plaid's test bank fixtures, or `production` once your app is approved.

Keys are stored in `EncryptedSharedPreferences` and never displayed back. To rotate, just paste a new value over the old one.

The bundled `server/` directory has a small Node.js proxy that exchanges public tokens for access tokens — required for any non-trivial use. See [server/README.md](server/README.md).

## Configuration knobs

| What                       | Where                                            |
| -------------------------- | ------------------------------------------------ |
| Foursquare key (build)     | `local.properties` → `FOURSQUARE_API_KEY`        |
| Foursquare key (runtime)   | Settings → Foursquare API key                    |
| Plaid client ID / secret   | Settings → Plaid keys                            |
| Build output directory     | Env var `ANDROID_BUILD_DIR` (overrides default)  |
| Map default location       | `RewardsMapScreen.kt` → `DEFAULT_LAT/LON`        |
| Default radius cascade     | `PlacesRepository.kt` → `nearby()` radii         |
| Sample spend for points    | `RewardsMapScreen.kt` → `SAMPLE_SPEND_DOLLARS`   |

## CI

GitHub Actions runs `./gradlew assembleDebug` on every push to `main`. The build directory is automatically redirected away from OneDrive on local Windows builds; CI uses the default `app/build`.

## Privacy

Everything sensitive stays on-device:

- Plaid access tokens, client ID, secret → `EncryptedSharedPreferences` (Tink + AES-256-GCM, master key in Android Keystore)
- Foursquare runtime key → same encrypted store
- Transaction cache → SQLCipher-encrypted Room DB
- PIN → BCrypt hash (cost 12)
- No analytics, no third-party crash reporters, no telemetry

The only outbound network calls are to: Plaid (banking), Foursquare (places), Overpass (places), CARTO (tiles), and Android's geocoder.

Full policy: [hbirring01.github.io/CreditCardApp/privacy.html](https://hbirring01.github.io/CreditCardApp/privacy.html)

## Releasing

See [RELEASING.md](RELEASING.md) — every release tags a version, refreshes screenshots, and updates this README.

## License

MIT — see [LICENSE](LICENSE) (add one if you plan to distribute).

## Acknowledgements

- [Plaid](https://plaid.com/) for the Link SDK
- [Foursquare](https://foursquare.com/developers/) for the Places API
- [OpenStreetMap contributors](https://www.openstreetmap.org/copyright) for Overpass data
- [CARTO](https://carto.com/attributions) for the Voyager tile style
- [osmdroid](https://github.com/osmdroid/osmdroid) for the Android map view
