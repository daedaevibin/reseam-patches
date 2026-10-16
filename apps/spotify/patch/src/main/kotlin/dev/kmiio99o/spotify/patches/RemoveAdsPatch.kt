package dev.kmiio99o.spotify.patches

import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import dev.kmiio99o.spotify.settings.SpotifySettings
import dev.kmiio99o.spotify.settings.SpotifyStrings

private val adTrack = method("createAdTrack") {
    strings("is_advertisement", "ad_id", "ad_playback_id")
    flags(AccessFlags.STATIC)
    returns("Lp/ahr0;")
}

val removeAds = patch("Remove ads") {
    description("Keeps ad metadata from being created when a session loads, so ad slots come back empty and no ad audio is prepared.")
    compatibleWith(SPOTIFY)
    settings(spotifySettings, section(SpotifyStrings.sectionAds, SpotifySettings.removeAds))

    execute {
        gate(SpotifySettings.removeAds) {
            adTrack.alwaysReturnNull()
        }
    }
}
