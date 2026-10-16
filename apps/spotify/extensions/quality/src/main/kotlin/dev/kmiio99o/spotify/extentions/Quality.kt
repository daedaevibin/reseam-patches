@file:JvmName("Quality")

package dev.kmiio99o.spotify.extentions

import app.reseam.runtime.Export
import dev.kmiio99o.spotify.settings.SpotifySettings

// Maps the stored quality choice to Spotify's internal tier value
// (0 unknown, 1 low, 2 normal, 3 high, 4 very high, 5 HiFi, 6 HiFi 24-bit).
private fun tier(): Int = when (SpotifySettings.qualityTier.value) {
    "normal" -> 2
    "high" -> 3
    "very_high" -> 4
    "hifi" -> 5
    "hifi24" -> 6
    else -> 3
}

@Export
fun forcedQuality(): Any = tier()

@Export
fun forcedTier(): Int = tier()
