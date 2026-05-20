# Releasing StashApp

Releases are **fully automated** by `.github/workflows/release.yml` — pushing a `vX.Y.Z` tag builds a signed release APK, generates the changelog from commit history, and publishes a GitHub Release with the APK attached.

Your job as the releaser is to bump the version, update the README "What's new" section, and tag.

## 1. Pre-flight

- [ ] CI on `main` is green (`./gradlew lintDebug testDebugUnitTest assembleDebug` all passing)
- [ ] `./gradlew assembleDebug` succeeds locally (sanity check on your dev box)
- [ ] Manual smoke test on a real device: launch → biometric → wallet → map (with at least one linked card) → settings
- [ ] No `TODO`/`FIXME` introduced in this cycle that blocks the release notes
- [ ] All three signing secrets are still set in repo settings — Actions **and** Dependabot buckets:
  - `SIGNING_KEYSTORE_BASE64`
  - `SIGNING_STORE_PASSWORD`
  - `SIGNING_KEY_PASSWORD`

## 2. Bump version

The workflow injects `versionName` from the git tag, so you only need to bump `versionCode`. Edit `app/build.gradle.kts`:

```kotlin
versionCode = (System.getenv("RELEASE_VERSION_CODE") ?: "<bump this>").toInt()
```

Or rely on the workflow setting `RELEASE_VERSION_CODE` from the tag — whichever convention you prefer. Currently the local default is hard-coded at the bottom of the fallback chain.

Semver guide (`MAJOR.MINOR.PATCH`):
- **PATCH** — bug fix only, install-compatible with previous version, same data
- **MINOR** — new feature, install-compatible, same data
- **MAJOR** — breaking change (e.g., applicationId rename in v1.8.0). The release workflow auto-injects an **"uninstall the old app before installing"** banner in the release notes whenever the rename commit isn't already in the previous tag.

## 3. Refresh screenshots (when UI changed)

Skip if no UI changed since the last release. Otherwise capture on a Pixel 6 / 7 emulator (1080×2400) in the default light theme.

| File                          | Screen                                                  |
| ----------------------------- | ------------------------------------------------------- |
| `screenshots/map.png`         | Rewards tab — map with the AI best-card hero visible    |
| `screenshots/offers.png`      | Offers tab — at least one card-linked offer card        |
| `screenshots/wallet.png`      | Wallet / cards home (at least 2 linked cards)           |
| `screenshots/settings.png`    | Settings — showing Plaid + Foursquare rows + version    |
| `screenshots/plaid.png`       | Plaid Link setup dialog                                 |

```powershell
adb exec-out screencap -p > screenshots/map.png
```

Keep filenames stable so READMEs keep rendering.

## 4. Update README

- [ ] **"What's new in v<X.Y.Z>"** block at the top — list user-visible changes
- [ ] Confirm the screenshot row still renders
- [ ] Update any feature bullets / config knob table that drifted

## 5. Update privacy policy (only if changed)

If this release adds a new third-party service, new permission, or changes what data is stored:

- [ ] Edit `docs/privacy.html`
- [ ] Bump the "Last updated" date
- [ ] GitHub Pages redeploys automatically on push to `main`

## 6. Commit, tag, push

```powershell
git add -A
git commit -m "release: v1.8.0 — <one-line summary>"
git tag v1.8.0
git push origin main
git push origin v1.8.0
```

Tag pushes trigger `release.yml`. Tag format **must** be `v<MAJOR>.<MINOR>.<PATCH>` — anything else won't match the workflow's `on: push: tags` filter.

## 7. Watch the workflow

- [ ] Open <https://github.com/hbirring01/Stash/actions/workflows/release.yml>
- [ ] Watch the run finish (~5 min). It will:
  - Decode the signing keystore from `SIGNING_KEYSTORE_BASE64`
  - Run `./gradlew assembleRelease` with `RELEASE_VERSION_NAME` injected from the tag
  - Build the changelog from commits between this tag and the previous tag
  - Publish a GitHub Release with `app-release.apk` attached
- [ ] If the workflow fails, fix forward — **never delete a tag** that already shipped to users

## 8. Post-release

- [ ] Verify the GitHub Pages site still loads: <https://hbirring01.github.io/Stash/>
- [ ] Smoke-test the published APK on a clean install (catches missing migrations)
- [ ] Close the milestone / move incomplete issues to next

---

### Why screenshots matter

A stale README is the #1 reason a Plaid production reviewer or a curious user bounces. Screenshots set expectations for the actual app surface and keep the docs honest about what shipped. Refreshing them is two minutes per release; skipping them compounds into a "what does this even look like now?" rewrite every six months.

### What the release workflow does NOT do

- It does **not** edit the README — you must commit your "What's new" entry *before* tagging
- It does **not** push the tag for you — that's your trigger
- It does **not** verify CI passed on `main` first — push tag from `main` only after green
- It does **not** create the milestone or close issues
