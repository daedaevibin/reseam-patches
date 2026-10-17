package dev.kmiio99o.spotify.patches.playback

import app.reseam.patch.field
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import dev.kmiio99o.spotify.patches.core.SPOTIFY
import dev.kmiio99o.spotify.patches.core.patchesPage
import dev.kmiio99o.spotify.patches.host.spotifySettings
import dev.kmiio99o.spotify.settings.SpotifySettings
import dev.kmiio99o.spotify.settings.SpotifyStrings

private val restrictions = klass("com.spotify.player.model.AutoValue_Restrictions")

private val emptySet = field("Lp/ohy0;", "t", "Lp/ohy0;")

val unlimitedSkips = patch("Unlimited skips") {
    description("Removes skip, swipe, and peek restrictions so you can skip freely, including in stations and other restricted mixes.")
    compatibleWith(SPOTIFY)
    settings(spotifySettings, section(patchesPage, SpotifyStrings.sectionPlayback, SpotifySettings.unlimitedSkips))

    execute {
        gate(SpotifySettings.unlimitedSkips) {
            restrictions.method("disallowSkippingNextReasons").before {
                returnValue(staticField(emptySet).cast("Lp/da60;"))
            }
            restrictions.method("disallowSkippingPrevReasons").before {
                returnValue(staticField(emptySet).cast("Lp/da60;"))
            }
            restrictions.method("disallowSwipeNextReasons").before {
                returnValue(staticField(emptySet).cast("Lp/da60;"))
            }
            restrictions.method("disallowSwipePrevReasons").before {
                returnValue(staticField(emptySet).cast("Lp/da60;"))
            }
            restrictions.method("disallowPeekingNextReasons").before {
                returnValue(staticField(emptySet).cast("Lp/da60;"))
            }
            restrictions.method("disallowPeekingPrevReasons").before {
                returnValue(staticField(emptySet).cast("Lp/da60;"))
            }
        }
    }
}
