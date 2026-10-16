package dev.kmiio99o.spotify.patches

import app.reseam.patch.after
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.settings.openSettings
import app.reseam.patch.settings.settingsHost

private val settingsPage = klass("settingsPage") {
    strings("parentDetails", "inviteParent", "storageLocation", "cacheLimit")
}

private val settingsPageCreate = settingsPage.method("create") {
    paramCount(0)
}

val spotifySettings = settingsHost("spotify") {
    compatibleWith(SPOTIFY)

    install {
        settingsPageCreate.after { openSettings() }
    }
}
