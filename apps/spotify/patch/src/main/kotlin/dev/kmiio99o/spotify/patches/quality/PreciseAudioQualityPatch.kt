package dev.kmiio99o.spotify.patches.quality

import app.reseam.patch.Type
import app.reseam.patch.field
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import dev.kmiio99o.spotify.extentions.bindings.Quality
import dev.kmiio99o.spotify.patches.core.SPOTIFY
import dev.kmiio99o.spotify.patches.core.patchesPage
import dev.kmiio99o.spotify.patches.host.spotifySettings
import dev.kmiio99o.spotify.settings.SpotifySettings
import dev.kmiio99o.spotify.settings.SpotifyStrings

private val meteredQualityGetterA = klass("Lp/wqp;").method("get") {
    paramCount(1)
    returns(Type.Object)
}

private val meteredQualityGetterB = klass("Lp/iq01;").method("get") {
    paramCount(1)
    returns(Type.Object)
}

private val nonMeteredQualityGetter = klass("nonMeteredQuality") {
    strings("streamNonMeteredQuality()I")
}.method("get") {
    paramCount(1)
    returns(Type.Object)
}

private val serializeSettings = klass("settingsStateSerializer") {
    strings("com.spotify.settings.rxsettings.SettingsState")
}.method("serialize") {
    paramCount(2)
}

private val streamQualityField = field("Lp/cd31;", "f", "I")
private val streamNonMeteredField = field("Lp/cd31;", "g", "I")

val forceAudioQuality = patch("Force audio quality") {
    description("Forces the streaming-quality tier the client reads and reports, up to HiFi 24-bit. The bitrate you actually get still depends on your plan and connection.")
    compatibleWith(SPOTIFY)
    settings(
        spotifySettings,
        section(patchesPage, SpotifyStrings.sectionQuality, SpotifySettings.forceQuality, SpotifySettings.qualityTier),
    )

    execute {
        gate(SpotifySettings.forceQuality) {
            meteredQualityGetterA.before {
                returnValue(Quality.forcedQuality())
            }
            meteredQualityGetterB.before {
                returnValue(Quality.forcedQuality())
            }
            nonMeteredQualityGetter.before {
                returnValue(Quality.forcedQuality())
            }
            serializeSettings.before {
                val state = param(1).cast("Lp/cd31;")
                state.set(streamQualityField, Quality.forcedTier())
                state.set(streamNonMeteredField, Quality.forcedTier())
            }
        }
    }
}
