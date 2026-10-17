package dev.kmiio99o.spotify.patches.premium

import app.reseam.patch.Type
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import dev.kmiio99o.spotify.patches.core.SPOTIFY
import dev.kmiio99o.spotify.patches.core.cleanupPage
import dev.kmiio99o.spotify.patches.host.spotifySettings
import dev.kmiio99o.spotify.settings.SpotifySettings
import dev.kmiio99o.spotify.settings.SpotifyStrings

private val productPredicates = klass("productPredicates") {
    strings("allow-child-video", "is-mobile-only-tier", "music-video-disabled", "nft-disabled", "has-audiobooks-available")
}

private val isPremium = productPredicates.method("N") {
    flags(AccessFlags.STATIC)
    paramCount(1)
    returns(Type.Boolean)
}

private val isFreeTier = productPredicates.method("G") {
    flags(AccessFlags.STATIC)
    paramCount(1)
    returns(Type.Boolean)
}

private val onDemandAllowed = productPredicates.method("S") {
    flags(AccessFlags.STATIC)
    paramCount(1)
    returns(Type.Boolean)
}

private val premiumTabConfig = klass("premiumTabConfig") {
    strings("premium_tab_enabled", "premium_tab_shows_on_tablets", "premium_tab_title")
}

val premiumSpoof = patch("Premium spoof") {
    description("Makes the client treat the account as Premium: hides the Premium tab in the navigation bar, hides upsell banners, nag screens and free-tier gates like shuffle-only. The server still enforces real entitlements, so downloads and some premium actions may not work.")
    compatibleWith(SPOTIFY)
    settings(spotifySettings, section(cleanupPage, SpotifyStrings.sectionPremium, SpotifySettings.spoofPremium))

    execute {
        gate(SpotifySettings.spoofPremium) {
            isPremium.before { returnTrue() }
            onDemandAllowed.before { returnTrue() }
            isFreeTier.before { returnFalse() }
            val tabFlags = premiumTabConfig.methods("premiumTabFlags") {
                paramCount(0)
                returns(Type.Boolean)
            }
            tabFlags.forEach {
                alwaysReturn(false)
            }
        }
    }
}
