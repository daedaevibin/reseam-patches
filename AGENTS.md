# AGENTS.md — Spotify patches for ReSeam

Authoritative working notes for this repo. Everything below was verified this session:
docs fetched from `https://reseam.app/docs/...`, SDK inspected with `javap`, APK facts proven
by `grep`/`diff` over decompiled sources. Do **not** re-explore docs/SDK/APK unless this file
marks something UNVERIFIED or you need an API not listed here.

## 1. Repo identity & layout

- Bundle: `spotify-patches` (`manifest.toml`: name / author / description / `format_version = 1`).
- Ships 6 patches + 1 settings host + 1 extension for Spotify Android — package `com.spotify.music`,
  version `com.spotify.music_9.1.90.2270-147078674_mi`.
- **User-mandated namespace: everything under `dev.kmiio99o.spotify.*`**
  - patches → `dev.kmiio99o.spotify.patches.*` (subpackages per group, see layout)
  - settings → `dev.kmiio99o.spotify.settings`
  - extension → `dev.kmiio99o.spotify.extentions` — **this spelling ("extentions") is intentional per user; do not "fix" it.**
- No `app.spotify.*` references may remain (last verified: zero).

```text
manifest.toml                                   bundle metadata
settings.gradle.kts                             rootProject.name = "Spotify patches"; app.reseam.workspace plugin v0.25.0
README.md                                       user-facing (patch list, build, release)
apps/spotify/patch/src/main/kotlin/dev/kmiio99o/spotify/patches/
    core/App.kt                                 val SPOTIFY = CompatiblePackage("com.spotify.music", listOf("9.1.90.2270"))
    core/Pages.kt                               patchesPage + cleanupPage (SettingsPage, shared by all patches)
    host/SettingsHost.kt                        settings host + entry hooks (navigation redirect + row rename)
    playback/UnlimitedSkipsPatch.kt             6 restriction getters → empty set
    ads/RemoveAdsPatch.kt                       ad-track factory → null
    quality/PreciseAudioQualityPatch.kt         3 registry getters + serializer field writes
    premium/PremiumSpoofPatch.kt                product-state predicates N/G/S → premium/free/off/false
    ui/DisableCanvasPatch.kt                    cw5 canvas flag → false
    ui/HideCreateButtonPatch.kt                 prefs read of create_button_enabled → false
apps/spotify/settings/
    build.gradle.kts                            reseamStrings { objectName = "dev.kmiio99o.spotify.settings.SpotifyStrings" }
    src/main/kotlin/dev/kmiio99o/spotify/settings/SpotifySettings.kt
    src/main/strings/values/strings.xml
apps/spotify/extensions/quality/src/main/kotlin/dev/kmiio99o/spotify/extentions/Quality.kt
spotify-apk/                                    decompiled APK sources (gitignored: `*-apk/`)
```

**Patch IDs changed in the v2 reorg** (subfolders = subpackages, e.g. `dev.kmiio99o.spotify.patches.playback.unlimitedSkips`).
Renaming files/packages again would reset users' settings/enablement — get user consent first.

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

- JDK 17, Android toolchain via Gradle, and network all work → **`./gradlew bundle` runs here**
  (prefix: `RESEAM_BUNDLE_KEY=$HOME/.reseam/bundle-signing.key ./gradlew bundle`).
- `reseam` CLI v0.25.0 lives at `/tmp/opencode/engine/reseam` (re-download if tmp wiped:
  `https://cdn.reseam.app/engine/v0.25.0/reseam-linux-x64`, `chmod +x`).
- SDK jar (re-extract if `/tmp/opencode/sdk` is gone):

```bash
mkdir -p /tmp/opencode/sdk && cd /tmp/opencode/sdk && unzip -o -q \
  ~/.gradle/caches/modules-2/files-2.1/app.reseam/reseam-patch-sdk/0.25.0/8d4c2fc59a335d8c6979df28588dd9752808f0d8/reseam-patch-sdk-0.25.0.jar
javap -cp . app.reseam.patch.CodeScope        # any SDK class
```

- APK sources: `spotify-apk/sources/sources` (jadx output; obfuscated classes in `p/`).
- Original APK for local tests: `spotify-apk/spotify.apk` (identical to the apkmirror file; 9.1.90, 17 dex).
- **Local patch test (verified working)** — bundle from `gh release download v0.0.1 -p spotify-patches.reseam`:

```bash
engine/reseam bundle list spotify-patches.reseam   # prints signer public-key hex = --trust value
engine/reseam patch spotify.apk --bundle spotify-patches.reseam \
  --trust <signer hex> --preset all --dry-run --output /tmp/out.apk   # validate only
# drop --dry-run for a real signed output APK
```

- Result (2026-10-16, after v2 reorg): **6/6 patches + settings host applied, 0 failed**, dry-run AND
  real signed run both pass (`--preset all`). Output zip-clean: 17→22 dexes; injected classes land in
  `classes21.dex` (`dev/kmiio99o/spotify/settings/SpotifySettings` + `SpotifyStrings`) and
  `classes22.dex` (`dev/kmiio99o/spotify/extentions/Quality`); `classes10/11.dex` gained one
  `kmiio99o` reference each (host bootstrap refs). Runtime behavior still needs a device (patched
  APK is re-signed → sideload requires uninstalling the store build, or use the Manager `--mount` flow).
- **Public key of the release signer** (from `bundle list`; share this, keep the private key safe):
  `b403855071d0fb1f18505ac980e040b0b30b3876a8b2d7f8d08798d5de5b2f55`

## 4. Canonical authoring shapes (docs-verified)

Every patch uses exactly this shape. Imports listed are the ones actually used.

```kotlin
val myPatch = patch("Patch name") {                       // app.reseam.patch.patch
    description("What it does.")
    compatibleWith(SPOTIFY)
    settings(spotifySettings, section(patchesPage, SpotifyStrings.sectionX, SpotifySettings.someToggle))
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
        settingsRowHook.before { openSettings() }         // top-level exts: app.reseam.patch.before,
    }                                                     // app.reseam.patch.settings.openSettings
}
```

- **Pages**: `SettingsPage("id", SpotifyStrings.pageX, order = n, summary = SpotifyStrings.pageXSummary)`
  — declared as top-level vals in `core/Pages.kt`; pass page as first arg of `section(...)`.
  Sections with same page+title merge; pages without running-patch settings are omitted.
- **Discovery**: bundle discovery scans **top-level `public val`s of type `ReseamPatch`** (and hosts).
  Patches/hosts must be top-level public vals. Package name is irrelevant to discovery (but IS woven
  into the patch ID).
- No `brand()`; hosts have no `description()`.
- **Receiver vs top-level**: inside `gate {}` (receiver scope)
  `target.before { }` / `.after { }` / `.alwaysReturnNull()` resolve to the scope's **member** extensions —
  do NOT import `HooksKt`-style top-level extensions there. Inside `install {}`, top-level extensions
  imported from `app.reseam.patch` work (build-verified both ways).
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

### Patch 5 — Premium spoof ✅
- `p/n9k1` (abstract, static utility `bxh1`) = THE product-state predicate class. Every free/premium
  UI decision funnels through its static `(Map) → boolean` methods (grep-proven callers: a000, ae00,
  n550, tw00, cbu0, aq0...).
  - `F(map)` = `l("allow-child-video", true)` · `G(map)` = `catalogue == "free"` (isFree)
  - `H(map)` = `horizon == "FULL"` · `I(map)` = pick-and-shuffle ✅ · `J/K/L` = mobile-only-tier /
    music-video-disabled / nft-disabled · `N(map)` = `"premium".equals(type)` (isPremium)
  - `P(str)` = null→true else hash-check of streaming-rules string (`d340.a.M(str).b() == 1929113152`)
  - `S(map)` = `!P(map.get("streaming-rules"))` (on-demand allowed)
- Class anchor: `klass("productPredicates") { strings("allow-child-video","is-mobile-only-tier",
  "music-video-disabled","nft-disabled","has-audiobooks-available") }` — each string appears in ≤2
  classes but only n9k1 has all five (AND across methods → unique).
- Method anchors (within class, grep-proven unique): N = `strings("premium","type")` — "premium" ×1
  in class; G = `strings("catalogue","free")`; S = `strings("streaming-rules")`. All
  `flags(STATIC); paramCount(1); returns(Type.Boolean)`.
- Hook: N → `returnTrue()`, S → `returnTrue()`, G → `returnFalse()`.
- **Honest scope**: client-side only (server enforces real entitlements; downloads may fail).
  Product-state map itself (proto `EsSession$ProductStateMap` → `u7y/gos0`) NOT touched.

### Patch 6 — Hide Create button ✅
- Stock toggle exists in Spotify settings (`lp.java:173` row `createButton` → `jg91(14)`) but user
  wants it in our bundle.
- Storage: `r9m` — `dz61.c = dz61.b.l("create_button_enabled")` static key;
  `r9m.b` = `fz61.h(key, true)` observable. `fz61` = typed-prefs interface, `kz61` = **only impl**.
  Anchor: `klass("prefsStore") { strings("key ", " has no value") }` — both literals in kz61 only
  (grep-proven ≤kz61 for ` has no value`).
- Target: `method("h") { paramCount(2); returns(Type.Boolean) }` (getBoolean(key, def));
  `param(0)` = key instance → `param(0).field(field("Lp/dz61;","a","Ljava/lang/String;"))`
  compared to `"create_button_enabled"` → `returnFalse()`. All boolean prefs reads hit one
  whenEqual — cheap. Stock settings toggle then reads off (consistent UI).

### Settings entry ✅ (v2 — replaced j4k.create() overlay)
- v1 (removed): `j4k.create().after { openSettings() }` — j4k is a merged multi-section factory;
  `lp.java` (main settings page builder) calls some `ka31.create()` **eagerly for section icons**,
  so the overlay popped as soon as Spotify's settings opened = "injected into the settings button".
- v2 (shipped): two host hooks:
  1. `method("navigateToUri") { strings("navigateToUriWithNullableUbiInstrumentation"); paramCount(4) }`
     — `pgk1.m` is THE spotify-URI navigation funnel (`ejl0.h(str)` etc.); string is its stack-trace
     filter constant, grep-unique. `.before { whenEqual(param(1), string("spotify:internal:licenses"))
     { openSettings(); returnVoid() } }` — tapping the **Licenses** row opens our settings INSTEAD
     of the licenses page (param(0) = the `dz61` key of row's URI? no — param(1) = URI string;
     sig `(ejl0, String, fbv, Bundle)`).
  2. Row relabel: `klass("Lp/w731;").method("a") { paramCount(1); returns(String) }` — w731.a(we20)
     resolves a settings row's title via `zjk1.s(fr71.c, fr71.d, we20)`. `.before`: walk
     `thisObject.a.b` (`field("Lp/w731;","a","Lp/u731;")` → `field("Lp/u731;","b","Lp/fr71;")` →
     `field("Lp/fr71;","a","Ljava/lang/String;")`), whenEqual to `"licenses"` →
     `returnValue(string("ReSeam patches"))`. Res ID `2131961449` used ONLY by the licenses row
     (grep-proven) — no collateral renames.
- Result: About settings has a row labeled "ReSeam patches"; tapping it opens the bundle settings
  (overlay, not native subpage). Nothing else hijacked; licenses page unreachable (acceptable,
  documented in README).

### Recurring grep probes (cheap re-verification)

```bash
cd spotify-apk/sources/sources
grep -rn "ad_playback_id" .            # ads anchor uniqueness
grep -rn "streamNonMeteredQuality()I" . # hq01 uniqueness
grep -rn "com.spotify.settings.rxsettings.SettingsState" .  # ad31 uniqueness
grep -rn "canvas_enabled" .            # canvas flags
grep -rn "create_button_enabled" .     # create button pref key
grep -rn "spotify:internal:licenses" . # entry row URI + route table
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
  property + `_summary`, plus `section_*` and `page_*`.
- Keep settings copy honest (e.g. quality + premium summaries state server still enforces).
- README patch list and `manifest.toml` description must stay in sync with actual patches (6 now).

## 10. Docs map (fetch only if something is missing above)

- start / first patch: `https://reseam.app/docs/authoring/start/`, `.../first_patch/`
- patch anatomy: `.../patches/` · settings API: `.../settings/`
- finding targets: `.../finding_code/` · hook points: `.../points/`
- mutating code: `.../changing_code/` · bindings: `.../bindings/` · extensions: `.../extensions/`
- API reference (authoritative for signatures): `.../reference/`

## Open items

- Build + patch test pass (build 2026-10-16, applied=6 failed=0). Device behavior still unverified:
  premium-spoof side effects (server interactions with spoofed entitlements), entry-row visibility,
  Create-button hiding, licenses-row rename. Test on device before publishing an announcement.
- Serialize hook writes `public final int` fields via `ValueRef.set` — no rejection in SDK, and the
  real patch run passed; runtime write is still only proven once used on a device.
- w731 title hook walks 3 fields (`a→b→a`) — if a future update changes row nesting, query still
  matches but field walk could NPE at runtime; loud-fail at patch time is NOT guaranteed here.
  (Mitigation: version pin catches most drift.)
- Manager-side: settings/enablement reset for existing users (IDs changed in v2 reorg).
