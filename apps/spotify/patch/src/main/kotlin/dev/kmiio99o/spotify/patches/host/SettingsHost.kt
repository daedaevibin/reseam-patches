package dev.kmiio99o.spotify.patches.host

import app.reseam.patch.*
import app.reseam.patch.settings.openSettings
import app.reseam.patch.settings.settingsHost
import dev.kmiio99o.spotify.patches.core.SPOTIFY

private val navigateToUri = method("navigateToUri") {
    strings("navigateToUriWithNullableUbiInstrumentation")
    paramCount(4)
}

private val mainPageFactory = method("mainPageFactory") {
    strings("createButton", "trailingActions")
    paramCount(0)
}

private val rowFactory = klass("Lp/fr71;").method("<init>")
private val rowIconField = field("Lp/i660;", "J0", "Lp/i660;")
private val clickFactory = klass("Lp/khl0;").method("<init>")
private val actionFactory = klass("Lp/dhl0;").method("<init>")
private val navFactory = klass("Lp/hhl0;").method("<init>") { hasParam("Ljava/lang/String;") }
private val rowsGroupFactory = klass("Lp/fc31;").method("<init>")
private val groupsField = field("Lp/ja31;", "d", "Ljava/util/List;")

private fun CodeScope.resIntArg(factory: MethodTarget, slot: Int, value: Int): ValueRef =
    if (factory.parameterTypes.getOrNull(slot) == "Ljava/lang/Integer;")
        callStatic("Ljava/lang/Integer;", "valueOf", "(I)Ljava/lang/Integer;", int(value))
    else int(value)

val spotifySettings = settingsHost("spotify") {
    compatibleWith(SPOTIFY)

    install {
        navigateToUri.before {
            whenEqual(param(1), string("spotify:internal:licenses")) {
                openSettings()
                returnVoid()
            }
        }
        // Row injection disabled; access via Licenses tap only
    }
}
