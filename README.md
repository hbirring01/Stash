# StashApp — Best Credit Card Nearby

Android app that tells you **which credit card to swipe at the business in front of you** for the highest rewards. Pulls your linked cards via Plaid, finds nearby businesses via Foursquare + OpenStreetMap, and ranks every visible place by the multiplier your cards earn there.

<p align="center">
  <img src="screenshots/map.png"      width="220" alt="Rewards tab — map with AI best-card hero" />
  <img src="screenshots/offers.png"   width="220" alt="Offers tab — card-linked issuer offers" />
  <img src="screenshots/wallet.png"   width="220" alt="Wallet / cards home" />
  <img src="screenshots/settings.png" width="220" alt="Settings — keys, theme, version" />
</p>

<p align="center">
  <a href="https://hbirring01.github.io/Stash/privacy.html"><b>Privacy policy</b></a> ·
  <a href="RELEASING.md">Release checklist</a> ·
  <a href="server/README.md">Plaid proxy server</a>
</p>

---

## What's new in v1.7.7

- ⚡ **Faster map business lookup** — the Rewards tab now feels snappy when you open it or pan to a new area:
  - **Overpass mirrors race in parallel** instead of sequentially. The fastest of the three OSM mirrors wins; if one is slow or returning 504, you no longer wait for its timeout before the next is tried.
  - **In-memory cache** for `nearby()` results (rounded to ~110 m, 5-minute TTL, 32 entries). Rapid re-opens, zoom-outs, and small pans hit the cache instead of refiring the network.
  - **Parallelized data load** in `RewardsMapViewModel.applyLocation`: cards, rotating bonuses, unactivated offers, and the places call now run concurrently rather than one-after-another.
  - **Tighter Overpass QL timeout** (25s → 15s) so slow mirrors fail fast and the race resolves sooner.

## What's new in v1.7.6

- ↩️ **Undo on usage delete** — deleting a statement-credit usage now shows a "Usage deleted" snackbar with an **Undo** action. Undo restores the original row (preserving its `MANUAL` / `AUTO` / `AI` source and timestamp) and, for auto-logged rows, removes the dismissal so the matcher's state stays consistent.
- 🔁 **Manual rescan per credit** — the credit row's overflow menu now has a **Rescan** action for auto-tracked credits. Re-runs `StatementCreditAutoMatcher.rescanForCredit` against that card's transaction history without waiting for the next Plaid sync — useful after editing match rules or when a transaction posts late.

## What's new in v1.7.5

- 🗝️ **Foursquare onboarding banner** — when the Rewards map empty state shows up and you haven't configured a Foursquare API key yet, a "Coverage looks thin here" card explains why (OSM-only is sparse in suburbs/rural areas) and offers an **Open Settings** chip that deep-links straight to the Settings page so you can paste a free-tier key.
- 🔁 **AI retry/backoff** — `AiMatchClient` now retries transient `429` (rate-limited) and `503` (overloaded) responses with exponential backoff (500 ms → 1 s → 2 s, up to 3 attempts) and honors any server-provided `Retry-After` header. Most free-tier bursts now resolve silently instead of giving up after one try.

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
git clone https://github.com/hbirring01/Stash.git
cd Stash
```

Create `local.properties` (already git-ignored) with at minimum:

```properties
sdk.dir=C:\\Users\\<you>\\AppData\\Local\\Android\\Sdk

# Optional — bakes a default Foursquare key into the build. Users can override
# it at runtime under Settings → Foursquare API key (stored encrypted).
# Get a free key at https://foursquare.com/developers/
FOURSQUARE_API_KEY=fsq3YOUR_KEY_HERE

# Optional — release signing. CI reads these from repo secrets; for local
# release builds, set them here. Debug builds work without them (the gradle
# config falls back to an unsigned debug keystore).
# SIGNING_STORE_FILE=upgrade.keystore
# SIGNING_STORE_PASSWORD=…
# SIGNING_KEY_ALIAS=upgrade
# SIGNING_KEY_PASSWORD=…
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

The bundled `server/` directory is an **optional hardened Plaid proxy** that lets you keep your `client_id` / `secret` off the device entirely. The app currently talks to Plaid directly; the proxy is included as scaffolding if you ever want to migrate. See [server/README.md](server/README.md).

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

Full policy: [hbirring01.github.io/Stash/privacy.html](https://hbirring01.github.io/Stash/privacy.html)

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
