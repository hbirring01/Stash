# Releasing StashApp

Every release ships a tagged APK, refreshed screenshots, and an updated README. Follow this checklist end-to-end — skipping the screenshot step is the most common miss.

## 1. Pre-flight

- [ ] `./gradlew assembleDebug` is green locally
- [ ] CI on `main` is green
- [ ] Manual smoke test on a real device: launch → biometric → wallet → map (with at least one linked card) → settings
- [ ] No `TODO`/`FIXME` introduced in this cycle that block the release notes

## 2. Bump version

Edit `app/build.gradle.kts`:

```kotlin
versionCode = <previous + 1>
versionName = "1.3.2"   // semver: MAJOR.MINOR.PATCH
```

## 3. Refresh screenshots

Capture on a Pixel 6 / 7 emulator (1080×2400) with the default light theme. Use the **same device** every release for visual consistency.

Required shots (overwrite the existing PNGs — keep filenames stable so READMEs keep working):

| File                          | Screen                                                  |
| ----------------------------- | ------------------------------------------------------- |
| `screenshots/map.png`         | Rewards tab — map with the AI best-card hero visible    |
| `screenshots/offers.png`      | Offers tab — at least one card-linked offer card        |
| `screenshots/wallet.png`      | Wallet / cards home (at least 2 linked cards)           |
| `screenshots/settings.png`    | Settings — showing Plaid + Foursquare rows + version    |
| `screenshots/plaid.png`       | Plaid Link setup dialog                                 |
| `screenshots/add.png`         | Add card screen (optional, only if it changed)          |
| `screenshots/empty.png`       | Empty / first-run state (optional, only if it changed)  |

Capture command on a connected device:

```powershell
adb exec-out screencap -p > screenshots/map.png
```

Then trim status bar / nav bar if needed and commit.

## 4. Update README

- [ ] **"What's new" section** at the top — list the user-visible changes in this version
- [ ] Confirm the 3-up screenshot row still renders (`map.png`, `wallet.png`, `settings.png`)
- [ ] Update any feature bullets, architecture diagram, or config-knob table that drifted

## 5. Update privacy policy if needed

If this release adds a new third-party service, new permission, or changes what data is stored:

- [ ] Edit `docs/privacy.html`
- [ ] Bump the "Last updated" date
- [ ] Commit — GitHub Pages will redeploy on push to `main`

## 6. Commit, tag, push

```powershell
git add -A
git commit -m "release: v1.3.2 — <one-line summary>"
git tag v1.3.2
git push origin main --tags
```

## 7. GitHub release

- [ ] Open https://github.com/hbirring01/CreditCardApp/releases/new
- [ ] Select the new tag
- [ ] Title: `v1.3.2 — <one-line summary>`
- [ ] Paste the "What's new" bullets from README
- [ ] Attach the signed APK if you produced one
- [ ] Publish

## 8. Post-release

- [ ] Verify the GitHub Pages site still loads: <https://hbirring01.github.io/CreditCardApp/>
- [ ] Smoke-test the release APK on a clean install (catches missing migrations)
- [ ] Close the milestone / move incomplete issues to next

---

### Why screenshots matter

A stale README is the #1 reason a Plaid production reviewer or a curious user bounces. Screenshots set expectations for the actual app surface and keep the docs honest about what shipped. Refreshing them is two minutes per release; skipping them compounds into a "what does this even look like now?" rewrite every six months.
