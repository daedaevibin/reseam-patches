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
        val titleId = resources.addString("reseam_patches_title", "ReSeam patches")?.toInt() ?: error("addString reseam_patches_title failed")
        val summaryId = resources.addString("reseam_patches_title_summary", "ReSeam patch bundle settings")?.toInt() ?: error("addString reseam_patches_title_summary failed")

        navigateToUri.before {
            whenEqual(param(1), string("spotify:internal:licenses")) {
                openSettings()
                returnVoid()
            }
        }

        mainPageFactory.after {
            val rowsListField = klass(rowsGroupFactory.owner).fieldOfType("Lp/m960;")
            val groups = this.result.field(groupsField)
            whenEqual(groups.callInterface("Ljava/util/List;", "size", "()I"), int(3)) {
                val group = groups
                    .callInterface("Ljava/util/List;", "get", "(I)Ljava/lang/Object;", int(2))
                    .cast(rowsGroupFactory.owner)
                val click = newInstance(
                    clickFactory.owner, clickFactory.proto,
                    newInstance(actionFactory.owner, actionFactory.proto, int(35), bool(false)),
                    newInstance(navFactory.owner, navFactory.proto, string("spotify:internal:licenses"), nullObject),
                )
                val row = newInstance(
                    rowFactory.owner, rowFactory.proto,
                    string("reseamPatches"),
                    resIntArg(rowFactory, 1, titleId),
                    nullObject,
                    resIntArg(rowFactory, 3, summaryId),
                    nullObject, nullObject, nullObject, nullObject,
                    staticField(rowIconField),
                    nullObject,
                    click,
                    nullObject,
                    int(22506),
                )
                val rows = group.field(rowsListField)
                val copy = newInstance("Ljava/util/ArrayList;", "(Ljava/util/Collection;)V", rows.cast("Ljava/util/Collection;"))
                copy.callVirtual("Ljava/util/ArrayList;", "add", "(Ljava/lang/Object;)Z", row)
                group.set(rowsListField, copy)
            }
        }
    }
}
