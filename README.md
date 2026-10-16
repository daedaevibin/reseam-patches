<p align="center">
  <img src="https://reseam.app/logo.svg" alt="Reseam logo" width="96">
</p>

<h1 align="center">Spotify patches for Reseam</h1>

An original [Reseam](https://reseam.app) patch bundle for the Spotify Android app: unlimited skips, ad removal, forced audio quality, and a settings screen inside Spotify's own settings.

It ships four patches, each with its own in-app toggle:

- `Unlimited skips` removes skip, swipe, and peek restrictions.
- `Remove ads` keeps ad metadata from being created when a session loads.
- `Force audio quality` forces the streaming-quality tier the client reads and reports, up to HiFi 24-bit.
- `Disable Canvas` stops Canvas video loops from loading in the now-playing view.

The [docs](https://reseam.app/docs/authoring/start/) explain how bundles are built.

## Setup

- JDK 17.
- Android SDK, with `ANDROID_HOME` pointing at it.
- The `reseam` CLI, on `PATH` or at the path in `RESEAM_BIN`. Use the version that matches the plugin version in `settings.gradle.kts`. Releases ship Linux x64 and Windows x64 binaries; on other platforms [build it from source](https://reseam.app/docs/cli/install/).
- A signing key: `reseam bundle keygen --out ~/.reseam/bundle-signing.key`. It prints the public key users will trust. Keep the key file private and out of the repository.

## Layout

```text
manifest.toml                     bundle name, author, description
settings.gradle.kts               Reseam plugin version
apps/<app>/patch/                 Kotlin patches, one module per app
apps/<app>/extensions/<name>/     Kotlin code injected into the app
apps/<app>/settings/              in-app settings and translated strings (optional)
shared/<name>/                    extensions used by more than one app
```

Modules need no build script. The `app.reseam.workspace` plugin configures them from the layout. How to write patches is in [Your first patch](https://reseam.app/docs/authoring/first_patch/).

## Build and test

```shell
./gradlew bundle
reseam bundle list build/reseam/spotify-patches.reseam
reseam patch app.apk \
  --bundle build/reseam/spotify-patches.reseam \
  --trust <public key> \
  --enable "Unlimited skips" \
  --output patched.apk
```

## Release

The workflow in `.github/workflows/release.yml` runs on `v*` tags. It builds and signs the bundle, adds the release to the `patches.json` of your latest release with the commit subjects since the previous tag as its notes, and attaches both to a GitHub release.

1. Add the signing key as the repository secret `BUNDLE_SIGNING_KEY_B64`: `base64 -w0 ~/.reseam/bundle-signing.key`.
2. Push a tag: `git tag v0.1.0 && git push origin v0.1.0`.

Share your patch source with this link. It opens Reseam Manager, which shows the source's apps, patches and signing key before adding it, and keeps it up to date from your latest release. Every release lists the link too.

```text
https://reseam.app/add/?url=https%3A%2F%2Fgithub.com%2F<owner>%2F<repo>%2Freleases%2Flatest%2Fdownload%2Fpatches.json
```

Without Reseam Manager, the link opens a page with the steps to add the source. Publish your public key where users can check it against the one Reseam Manager shows.

When you move to a new Reseam version, bump the plugin version in `settings.gradle.kts`. The release workflow downloads the CLI of that same version.

## License

AGPL-3.0-or-later, with additional terms under section 7 in [NOTICE](NOTICE).
