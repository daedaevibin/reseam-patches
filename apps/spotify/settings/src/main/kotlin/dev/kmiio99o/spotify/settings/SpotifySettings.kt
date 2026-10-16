package dev.kmiio99o.spotify.settings

import app.reseam.patch.settings.Choice
import app.reseam.patch.settings.SettingsGroup
import app.reseam.patch.settings.choice
import app.reseam.patch.settings.toggle

object SpotifySettings : SettingsGroup(SpotifyStrings) {
    val unlimitedSkips by toggle(default = true)
    val removeAds by toggle(default = true)
    val disableCanvas by toggle(default = true)
    val forceQuality by toggle(default = false)
    val qualityTier by choice(
        default = "high",
        choices = listOf(
            Choice("normal", SpotifyStrings.qualityNormal),
            Choice("high", SpotifyStrings.qualityHigh),
            Choice("very_high", SpotifyStrings.qualityVeryHigh),
            Choice("hifi", SpotifyStrings.qualityHifi),
            Choice("hifi24", SpotifyStrings.qualityHifi24),
        ),
    )
}
