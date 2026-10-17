package dev.kmiio99o.spotify.patches.core

import app.reseam.patch.settings.SettingsPage
import dev.kmiio99o.spotify.settings.SpotifyStrings

val patchesPage = SettingsPage(
    id = "patches",
    title = SpotifyStrings.pagePatches,
    order = 0,
    summary = SpotifyStrings.pagePatchesSummary,
)

val cleanupPage = SettingsPage(
    id = "cleanup",
    title = SpotifyStrings.pageCleanup,
    order = 1,
    summary = SpotifyStrings.pageCleanupSummary,
)
