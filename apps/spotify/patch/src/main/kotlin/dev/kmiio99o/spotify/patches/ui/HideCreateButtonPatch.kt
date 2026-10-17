package dev.kmiio99o.spotify.patches.ui

import app.reseam.patch.Type
import app.reseam.patch.fieldOfType
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

private val prefsStore = klass("prefsStore") {
    strings("key ", " has no value")
}

private val getBoolean = prefsStore.method("h") {
    paramCount(2)
    returns(Type.Boolean)
}

val hideCreateButton = patch("Hide Create button") {
    description("Keeps the Create shortcut button on the Home tab hidden by reporting its setting as off.")
    compatibleWith(SPOTIFY)
    settings(spotifySettings, section(cleanupPage, SpotifyStrings.sectionInterface, SpotifySettings.hideCreateButton))

    execute {
        gate(SpotifySettings.hideCreateButton) {
            val prefKeyClass = klass(getBoolean.parameterTypes[0])
            getBoolean.before {
                whenEqual(param(0).field(prefKeyClass.fieldOfType("Ljava/lang/String;").ref), string("create_button_enabled")) {
                    returnFalse()
                }
            }
        }
    }
}
