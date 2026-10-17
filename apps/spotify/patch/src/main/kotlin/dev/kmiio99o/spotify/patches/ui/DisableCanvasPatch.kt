package dev.kmiio99o.spotify.patches.ui

import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import dev.kmiio99o.spotify.patches.core.SPOTIFY
import dev.kmiio99o.spotify.patches.core.cleanupPage
import dev.kmiio99o.spotify.patches.host.spotifySettings
import dev.kmiio99o.spotify.settings.SpotifySettings
import dev.kmiio99o.spotify.settings.SpotifyStrings

private val canvasEnabled = klass("Lp/cw5;").method("a") {
    paramCount(0)
    returns(Type.Boolean)
}

val disableCanvas = patch("Disable Canvas") {
    description("Stops Canvas video loops from loading and playing in the now-playing view, saving data and battery.")
    compatibleWith(SPOTIFY)
    settings(spotifySettings, section(cleanupPage, SpotifyStrings.sectionInterface, SpotifySettings.disableCanvas))

    execute {
        gate(SpotifySettings.disableCanvas) {
            canvasEnabled.before { returnFalse() }
        }
    }
}
