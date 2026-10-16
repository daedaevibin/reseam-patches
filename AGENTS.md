# AGENTS.md — Spotify patches for ReSeam

Authoritative working notes for this repo. Everything below was verified this session:
docs fetched from `https://reseam.app/docs/...`, SDK inspected with `javap`, APK facts proven
by `grep`/`diff` over decompiled sources. Do **not** re-explore docs/SDK/APK unless this file
marks something UNVERIFIED or you need an API not listed here.

## 1. Repo identity & layout

- Bundle: `spotify-patches` (`manifest.toml`: name / author / description / `format_version = 1`).
- Ships 4 patches + 1 settings host + 1 extension for Spotify Android — package `com.spotify.music`,
  version `com.spotify.music_9.1.90.2270-147078674_mi`.
- **User-mandated namespace: everything under `dev.kmiio99o.spotify.*`**
  - patches → `dev.kmiio99o.spotify.patches`
  - settings → `dev.kmiio99o.spotify.settings`
  - extension → `dev.kmiio99o.spotify.extentions` — **this spelling ("extentions") is intentional per user; do not "fix" it.**
- No `app.spotify.*` references may remain (last verified: zero).

```text
manifest.toml                                   bundle metadata
settings.gradle.kts                             rootProject.name = "Spotify patches"; app.reseam.workspace plugin v0.25.0
README.md                                       user-facing (patch list, build, release)
apps/spotify/patch/src/main/kotlin/dev/kmiio99o/spotify/patches/
    Spotify.kt                                  const val SPOTIFY = "com.spotify.music"
    SettingsHost.kt                             settings host + entry hook
    UnlimitedSkipsPatch.kt                      6 restriction getters → empty set
    RemoveAdsPatch.kt                           ad-track factory → null
    PreciseAudioQualityPatch.kt                 3 registry getters + serializer field writes
    DisableCanvasPatch.kt                       cw5 canvas flag → false
apps/spotify/settings/
    build.gradle.kts                            reseamStrings { objectName = "dev.kmiio99o.spotify.settings.SpotifyStrings" }
    src/main/kotlin/dev/kmiio99o/spotify/settings/SpotifySettings.kt
    src/main/strings/values/strings.xml
apps/spotify/extensions/quality/src/main/kotlin/dev/kmiio99o/spotify/extentions/Quality.kt
spotify-apk/                                    decompiled APK sources (gitignored: `*-apk/`)
```

## 2. Build rules (from template + git history — verified)

- **No `include(...)` anywhere.** The `app.reseam.workspace` settings plugin auto-discovers `apps/*`.
- **`patch/` and `extension/` modules have NO `build.gradle.kts`** ("Modules need no build script").
  Only the `settings/` module has one (the `reseamStrings { objectName = ... }` block).
- Inside `apps/*`, module wiring (settings ↔ patches ↔ extensions) is automatic; a module outside
  `apps/` would need `api(project(":settings"))`. Do not add build scripts "just in case".
- Strings: `reseamStrings { objectName = "<FQCN>" }` generates the strings object at that FQCN.
  Keep it in the same package as `SpotifySettings` (current: `dev.kmiio99o.spotify.settings`) so no import is needed.
- Build: `./gradlew bundle` → `build/reseam/spotify-patches.reseam`
  Apply: `reseam patch app.apk --bundle ... --trust <public key> --enable "<Patch name>" --output out.apk`
  Release: push to `main` → `.github/workflows/release.yml` auto-tags `v0.0.<run_number>` and
  publishes the GitHub release (secret `BUNDLE_SIGNING_KEY_B64`,
  keygen: `reseam bundle keygen --out ~/.reseam/bundle-signing.key`).

## 3. Toolchain available in THIS environment

- **No Android SDK, no `reseam` CLI, no `ANDROID_HOME` → cannot compile/patch here.**
  Substitute verification = javap + grep (see §7).
- SDK jar (re-extract if `/tmp/opencode/sdk` is gone):

```bash
mkdir -p /tmp/opencode/sdk && cd /tmp/opencode/sdk && unzip -o -q \
  ~/.gradle/caches/modules-2/files-2.1/app.reseam/reseam-patch-sdk/0.25.0/8d4c2fc59a335d8c6979df28588dd9752808f0d8/reseam-patch-sdk-0.25.0.jar
javap -cp . app.reseam.patch.CodeScope        # any SDK class
```

- APK sources: `spotify-apk/sources/sources` (jadx output; obfuscated classes in `p/`).

## 4. Canonical authoring shapes (docs-verified)

Every patch uses exactly this shape. Imports listed are the ones actually used.

```kotlin
val myPatch = patch("Patch name") {                       // app.reseam.patch.patch
    description("What it does.")
    compatibleWith(SPOTIFY)
    settings(spotifySettings, section(SpotifyStrings.sectionX, SpotifySettings.someToggle))
    execute {
        gate(SpotifySettings.someToggle) {                // app.reseam.patch.settings.gate
            someTarget.before { returnValue(...) }        // receiver style = GateScope member (works inside gate)
        }
    }
}
```

```kotlin
val spotifySettings = settingsHost("spotify") {           // app.reseam.patch.settings.settingsHost
    compatibleWith(SPOTIFY)
    install {
        settingsPageCreate.after { openSettings() }       // top-level exts: app.reseam.patch.after,
    }                                                     // app.reseam.patch.settings.openSettings
}
```

- **Discovery**: bundle discovery scans **top-level `public val`s of type `ReseamPatch`** (and hosts).
  Patches/hosts must be top-level public vals. Package name is irrelevant to discovery.
- No `brand()`; hosts have no `description()`.
- **Receiver vs top-level**: inside `gate {}`/`settingsHost {}`/`install {}` (receiver scope),
  `target.before { }` / `.after { }` / `.alwaysReturnNull()` resolve to the scope's **member** extensions —
  do NOT import `HooksKt`-style top-level extensions for those. Outside a receiver scope, import
  top-level extensions (`app.reseam.patch.after`, etc.).
- Settings registration order = order shown to the user; one `section(...)` per patch.

### Settings group

```kotlin
import app.reseam.patch.settings.Choice
import app.reseam.patch.settings.SettingsGroup
import app.reseam.patch.settings.choice
import app.reseam.patch.settings.toggle

object SpotifySettings : SettingsGroup(SpotifyStrings) {
    val someToggle by toggle(default = true)                       // key = snake_case(propertyName)
    val qualityTier by choice(default = "high", choices = listOf(Choice("high", SpotifyStrings.qualityHigh)))
}
```

- `SettingDelegate` derives the key from the property name (`forceQuality` → `force_quality`);
  strings.xml MUST contain that key **and** `<key>_summary`.
- Read from extensions/patches: `SpotifySettings.someToggle.value`, `SpotifySettings.qualityTier.value`.
- Section titles: `section_x` key → `SpotifyStrings.sectionX`. Choice labels: own strings, passed into `Choice`.
- Toggles/choice imports come from `app.reseam.patch.settings.*` (easy to forget — caused a prior bug).

## 5. SDK reference (all javap-verified against 0.25.0)

### Target queries — the label-vs-name trap

| Call | First String arg means | Use for |
|---|---|---|
| `klass("Lp/wqp;")` | **real class-name filter** (normalized via `TypesKt.descriptor`) | obfuscated/exact classes |
| `klass("debugLabel") { ClassQuery }` | **debug label ONLY** (no name filtering!) | content-anchored classes |
| `ClassTarget.method("name")` chained | **method-name filter**; block optional (default no-op via `method$default`) | known method names |
| top-level `method("label") { MethodQuery }` | **debug label ONLY**; filters come from the block | content-anchored methods |

- `ClassQuery.strings(vararg)` = AND across **all** methods of the class (`classHasString` iterates
  `getMethods()`). `MethodQuery.strings` is re-checked in `mismatch` → both are filters, not seeds.
- `MethodQuery`: `strings(vararg)`, `paramCount(n)`, `returns(String|Type)`, `flags(mask)`, `params()`
  (empty = no params).
- **`param(i)` is 0-based** (bytecode proof in `CodeEmitter.param`: bounds-check against declared
  params + static/instance register offset). E.g. `serialize(uuv, Object)` → payload = `param(1)`.
- `field(owner, name, type)` (`app.reseam.patch.field`) = field **reference without lookup** — never
  fails resolution. Use full descriptors: `field("Lp/cd31;", "f", "I")`.
- `flags(int)` is a **mask**: `app.reseam.patch.dex.AccessFlags.STATIC`.
- `app.reseam.patch.Type` = object of String constants: `Type.Object`, `Type.Boolean`, `Type.Void`, …
  (public static final Strings — usable anywhere a type String is accepted).

### CodeScope (hook body) — full op list (key ones bold)

- Values: `thisObject`, **`param(int)`**, `paramOfType`, `getLastParam`, `getResult`, `capture`,
  `local`, **`int(int)`**, `long`, **`bool(boolean)`**, `string`, `getNullObject`, `enumValue`,
  **`staticField(FieldTarget|FieldRef)`**.
- Writes: `setStatic(...)`, **`ValueRef.set(FieldTarget, ValueRef)`** (instance iput — no final-field
  rejection strings exist anywhere in the SDK; ART does not enforce `final` on iput), `ValueRef.cast`.
- Returns: **`returnValue(ValueRef)`**, `returnVoid()`, **`returnTrue()` / `returnFalse()` / `returnNull()`**.
- Branching: `whenTrue/whenFalse/whenNull/whenNotNull/whenEqual/whenNotEqual/whenInstanceOf` → `Otherwise`.
- Calls: `call(MethodTarget, …)`, `callStatic(owner, name, type, …)`, `newInstance`, `call(ExtMethod, …)`.
- Target hooks: `.before { }` (can emit a return → skips original body), `.after { }`,
  `.alwaysReturnNull()` (generic `alwaysReturn*` family exists).
- Gate: `gate(SettingDelegate) { … }` — hooks run only when the toggle is on.

### Extensions & bindings

- File: `@file:JvmName("Quality")` + `import app.reseam.runtime.Export` + `@Export fun name(...)`.
- Generated binding FQCN = **`<extension package>.bindings.<JvmName>`**:
  `dev.kmiio99o.spotify.extentions.bindings.Quality` (already imported that way in the quality patch).
- Extensions run **inside the app process** and read settings via `SpotifySettings.x.value`.
- JVM extensions have no lifecycle — keep them pure functions over settings values.

## 6. Spotify APK target map (9.1.90 — version-locked!)

Obfuscated `p.*` names are **version-locked**: a re-obfuscated update fails **loudly at patch time**
(query finds nothing), never silently. Prefer string/content anchors; when two classes are identical
dupes, anchor by exact class name and accept the lock (loud failure is the feature).

### Patch 1 — Unlimited skips ✅
- `com.spotify.player.model.AutoValue_Restrictions` (real name, stable; the ONLY Restrictions impl) —
  6 getters return `Lp/da60;`: `disallowSkippingNextReasons`, `disallowSkippingPrevReasons`,
  `disallowSwipeNextReasons`, `disallowSwipePrevReasons`, `disallowPeekingNextReasons`,
  `disallowPeekingPrevReasons`.
- Return: `staticField(field("Lp/ohy0;", "t", "Lp/ohy0;")).cast("Lp/da60;")` — the empty-set singleton.
  **Never call `Restrictions.EMPTY`** — it's an AutoValue instance → getter would recurse.

### Patch 2 — Remove ads ✅
- Top-level static factory `au0.a(jp0, uy0) → Lp/ahr0;` — uniquely matched by
  `method("createAdTrack") { strings("is_advertisement","ad_id","ad_playback_id"); flags(STATIC); returns("Lp/ahr0;") }`
  (`ad_playback_id` appears only in au0 — grep-proven).
- Caller `hn61:635-653` already has a null path → `alwaysReturnNull()` is safe.

### Patch 3 — Force audio quality ✅ (two layers; both needed)
- Registry: `kq01.g = lq01("stream_quality", iq01.h)`, `kq01.h = lq01("stream_non_metered_quality", hq01.h)`;
  `lq01{a=key, b=e520}`; `zjv0.invoke` → virtual `get(obj)` → subclass override wins.
- Layer 1 — registry reads (settings UI + Rx flows):
  - `klass("Lp/wqp;")` and `klass("Lp/iq01;")` — **byte-identical dupes modulo name** (diff-proven) →
    content-anchor would be ambiguous → use per-class **name** anchors. Same for both.
  - `hq01` content-unique: `klass("nonMeteredQuality") { strings("streamNonMeteredQuality()I") }`.
  - all three: `.method("get") { paramCount(1); returns(Type.Object) }` → `returnValue(Quality.forcedQuality())`.
- Layer 2 — persistence/sync: `cd31` has NO `streamQuality()` method (R8-inlined); `ad31` (kotlinx
  serializer for cd31) reads `cd31.f`/`cd31.g` **directly** → getter hooks don't reach it.
  - Anchor: `klass("settingsStateSerializer") { strings("com.spotify.settings.rxsettings.SettingsState") }`
    `.method("serialize") { paramCount(2) }` → `param(1).cast("Lp/cd31;")`
    `.set(field("Lp/cd31;","f","I"), Quality.forcedTier())` + same for `"g"`.
  - Field order proven via `ad31.k()` descriptor: index5=`stream_quality`→f, index6=`stream_non_metered_quality`→g.
- Tier map (server proto `BitrateLevel` ↔ client `x2y` 0–6): 0 unknown, 1 low, 2 normal, 3 high,
  4 very_high, 5 hifi, 6 hifi24. Encoded in `Quality.kt` (`forcedQuality(): Any` for getters,
  `forcedTier(): Int` for iput).
- **Honest scope**: actual bitrate is server-decided (`pdk0`/`qsr0` map server → BitrateLevel).
  Client-side forcing affects what the client reads/reports/persists. Description says so.

### Patch 4 — Disable Canvas ✅
- `p/cw5` = canvas config holder; `cw5.a()` = `canvas_enabled` getter (delegating via `e()`).
- Chain: `cw5.a()` ← `mkc.a()` (`SDK_INT != 25 && this.d.a()`) → gates `mkc.b()` observable →
  canvas render pipeline. No consumer reads field `a` directly (grep-proven).
- Target: `klass("Lp/cw5;").method("a") { paramCount(0); returns(Type.Boolean) }.before { returnFalse() }`
  (`Lp/cw5;` exact descriptor ≠ `Lp/cw50;`). Other cw5 getters are internal flags only reachable
  after activation — leave alone.

### Settings entry ✅
- Factory `p/j4k.create()` (0-arg), uniquely AND-anchored by
  `klass("settingsPage") { strings("parentDetails","inviteParent","storageLocation","cacheLimit") }`
  (cacheLimit alone also matches rvr; AND → unique).
- Hook in host: `.after { openSettings() }` → ReSeam overlay appears when Spotify builds its
  settings pages. **v1 caveat**: overlay, dismissible — not a native subpage.

### Recurring grep probes (cheap re-verification)

```bash
cd spotify-apk/sources/sources
grep -rn "ad_playback_id" .            # ads anchor uniqueness
grep -rn "streamNonMeteredQuality()I" . # hq01 uniqueness
grep -rn "com.spotify.settings.rxsettings.SettingsState" .  # ad31 uniqueness
grep -rn "canvas_enabled" .            # canvas flags
diff <(sed 's/wqp/CCLS/g' p/wqp.java) <(sed 's/iq01/CCLS/g' p/iq01.java)  # dupe check
```

## 7. Deferred work — closed questions (don't re-research blindly)

| Item | Status | Evidence |
|---|---|---|
| M3 Expressive | **Impossible client-side** | `EXPRESSIVE_PLAYLISTS` lives in `pez` = **server metadata-extension enum** (`implements c770`, next to `CANVAS_V1`/`STORYLINES`), consumed via decoration policies. No client flag exists. |
| Canvas toggle | ✅ shipped as Disable Canvas | see Patch 4 |
| NPV/player UI variants | mostly non-actionable | flag configs exist (`fi4`/`gp4`/`er4`/`nj4`: floating_skip button, landscape NPV, promo indicator, seekbar easter eggs) but most default off or are server-set; `screenshot_detection_enabled` already defaults false (nothing to unlock). Only cosmetic candidate left: force `nj4` seekbar easter eggs ON. |
| Extra features | not researched | good next area only if user asks; start from `xjv0` flag configs in `m35`/`fi4` (alphabetical flag registry). |

## 8. Verification workflow (since no compiler here)

1. **Every** SDK call used → `javap` the interface first (`CodeScope`, `ClassTarget`, `ClassQuery`,
   `MethodQuery`, `ValueRef`, `SettingsGroup`, `Type`, `AccessFlags`).
2. Every anchor string → `grep -rn "<string>" spotify-apk/sources/sources` must be unique (or
   combined AND-query provably unique).
3. Read back each written file; grep repo for stale packages (`app.spotify` = forbidden).
4. If a build toolchain appears: `./gradlew bundle` is the first real gate; patch-time target
   failures are loud (query matches nothing) — that's the designed failure mode.
5. Never use Task/subagents (user rule). Never search the web for existing patches (user rule).
   `grep`/`rg` for APK searching (user rule).

## 9. Conventions & pitfalls

- No code comments unless asked (one pre-existing provenance comment set in `Quality.kt` — keep).
- No commits/pushes unless explicitly requested.
- Do not add `build.gradle.kts` to patch/extension modules; do not add `include()` lines.
- Toggle defaults: behavioral patches default `true` (installed = active); `forceQuality` is `false`
  because it has a dependent choice.
- strings.xml: Android-escaped, no raw apostrophes; keys exactly `snake_case` of the settings
  property + `_summary`, plus `section_*`.
- Keep settings copy honest (e.g. quality summary states bitrate still depends on plan/connection).
- README patch list and `manifest.toml` description must stay in sync with actual patches (4 now).

## 10. Docs map (fetch only if something is missing above)

- start / first patch: `https://reseam.app/docs/authoring/start/`, `.../first_patch/`
- patch anatomy: `.../patches/` · settings API: `.../settings/`
- finding targets: `.../finding_code/` · hook points: `.../points/`
- mutating code: `.../changing_code/` · bindings: `.../bindings/` · extensions: `.../extensions/`
- API reference (authoritative for signatures): `.../reference/`

## Open items

- No compile yet — run `./gradlew bundle` wherever JDK17 + Android SDK + reseam CLI exist.
- Unverified assumption (doc-stated): inside `apps/*`, patch modules automatically see the settings
  module without an explicit dependency. If the build says otherwise, add the workspace-documented
  wiring — not a hand-written build script unless docs say so.
- Serialize hook writes `public final int` fields via `ValueRef.set` — no rejection in SDK, but only
  a real patch run proves it. Fallback if it errors: drop Layer 2, keep getter hooks.
