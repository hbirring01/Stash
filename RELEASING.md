# Releasing StashApp

Every release ships a tagged APK, refreshed screenshots, and an updated README. Follow this checklist end-to-end — skipping the screenshot step is the most common miss.

## 1. Pre-flight

- [ ] `./gradlew assembleDebug` is green locally
- [ ] CI on `main` is green
- [ ] Manual smoke test on a real device: launch → biometric → wallet → map (with at least one linked card) → settings
- [ ] No `TODO`/`FIXME` introduced in this cycle that block the release notes

## 2. Pick the next tag

- [ ] Choose the next semver tag (for this cycle: `v1.5.0`)
- [ ] Do **not** edit `app/build.gradle.kts` — `versionName` / `versionCode` are injected by CI from the git tag during the release workflow
- [ ] Sanity-check the derived version code formula: `MAJOR * 10000 + MINOR * 100 + PATCH`

## 3. Refresh screenshots

Capture on a Pixel 6 / 7 emulator (1080×2400) with the default light theme. Use the **same device** every release for visual consistency.

Required shots (overwrite the existing PNGs — keep filenames stable so READMEs keep working):

| File                          | Screen                                                  |
| ----------------------------- | ------------------------------------------------------- |
| `screenshots/map.png`         | Rewards map with the AI best-card hero visible          |
| `screenshots/wallet.png`      | Wallet / cards home (at least 2 linked cards)           |
| `screenshots/settings.png`    | Settings screen showing Plaid + Foursquare key rows     |
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
git commit -m "release: v1.5.0 — <one-line summary>"
git tag v1.5.0
git push origin main --tags
```

Pushing the tag triggers `.github/workflows/release.yml`, which:

- builds the signed release APK,
- renames it to `StashApp-vX.Y.Z.apk`,
- generates release notes from the commits since the previous tag, and
- publishes the GitHub Release automatically.

## 7. Verify the GitHub release

- [ ] Open https://github.com/hbirring01/Stash/releases
- [ ] Confirm the new tag published as `StashApp v1.5.0`
- [ ] Verify the release body includes the expected "What's Changed" bullets
- [ ] Download the attached `StashApp-v1.5.0.apk` asset and confirm the SHA/size render correctly

## 8. Post-release

- [ ] Verify the GitHub Pages site still loads: <https://hbirring01.github.io/Stash/>
- [ ] Smoke-test the release APK on a clean install (catches missing migrations)
- [ ] Close the milestone / move incomplete issues to next

---

### Why screenshots matter

A stale README is the #1 reason a Plaid production reviewer or a curious user bounces. Screenshots set expectations for the actual app surface and keep the docs honest about what shipped. Refreshing them is two minutes per release; skipping them compounds into a "what does this even look like now?" rewrite every six months.
