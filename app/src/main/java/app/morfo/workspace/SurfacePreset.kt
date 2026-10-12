package app.morfo.workspace

import app.morfo.dentition.Arch
import app.morfo.dentition.Tooth
import app.morfo.math.Quat
import app.morfo.math.Vec3

/**
 * Standard views of a tooth. The camera always looks along -Z, so a preset is simply the tooth
 * rotation that turns the requested surface towards the viewer.
 *
 * Model frame (see tools/asset-pipeline): +Y superior, +Z facial, left teeth have mesial at -X.
 * Teeth stay in anatomical position (maxillary crowns down, mandibular crowns up), as in textbooks.
 */
enum class SurfacePreset(val labelEn: String) {
    FACIAL("BUCCAL"),
    LINGUAL("LINGUAL"),
    MESIAL("MESIAL"),
    DISTAL("DISTAL"),
    OCCLUSAL("OCCLUSAL");

    fun orientation(tooth: Tooth): Quat {
        val left = when (this) {
            FACIAL -> Quat.IDENTITY
            LINGUAL -> Quat.degrees(Vec3.Y, 180f)
            MESIAL -> Quat.degrees(Vec3.Y, 90f)
            DISTAL -> Quat.degrees(Vec3.Y, -90f)
            // Crown towards the viewer, facial surface at the bottom of the screen.
            OCCLUSAL -> if (tooth.arch == Arch.MANDIBULAR) {
                Quat.degrees(Vec3.X, 90f)
            } else {
                Quat.degrees(Vec3.Z, 180f) * Quat.degrees(Vec3.X, -90f)
            }
        }
        return if (tooth.isMirrored) left.mirrorX() else left
    }

    /** Polish term, adjusted to the teeth it applies to. */
    fun labelPl(teeth: Collection<Tooth>): String {
        val anterior = teeth.isNotEmpty() && teeth.all { it.kind.isAnterior }
        val posterior = teeth.isNotEmpty() && teeth.none { it.kind.isAnterior }
        val upper = teeth.isNotEmpty() && teeth.all { it.arch == Arch.MAXILLARY }
        val lower = teeth.isNotEmpty() && teeth.all { it.arch == Arch.MANDIBULAR }
        return when (this) {
            FACIAL -> when { anterior -> "Wargowa"; posterior -> "Policzkowa"; else -> "Przedsionkowa" }
            LINGUAL -> when { upper -> "Podniebienna"; lower -> "Językowa"; else -> "Jęz./podn." }
            MESIAL -> "Mezjalna"
            DISTAL -> "Dystalna"
            OCCLUSAL -> when { anterior -> "Sieczna"; posterior -> "Żująca"; else -> "Żująca/sieczna" }
        }
    }

    fun labelEn(teeth: Collection<Tooth>): String {
        val anterior = teeth.isNotEmpty() && teeth.all { it.kind.isAnterior }
        val posterior = teeth.isNotEmpty() && teeth.none { it.kind.isAnterior }
        val upper = teeth.isNotEmpty() && teeth.all { it.arch == Arch.MAXILLARY }
        val lower = teeth.isNotEmpty() && teeth.all { it.arch == Arch.MANDIBULAR }
        return when (this) {
            FACIAL -> when { anterior -> "LABIAL"; posterior -> "BUCCAL"; else -> "FACIAL" }
            LINGUAL -> when { upper -> "PALATAL"; lower -> "LINGUAL"; else -> "LINGUAL" }
            OCCLUSAL -> when { anterior -> "INCISAL"; posterior -> "OCCLUSAL"; else -> "OCCL./INC." }
            else -> labelEn
        }
    }

    fun labelEnTitle(teeth: Collection<Tooth>): String {
        val anterior = teeth.isNotEmpty() && teeth.all { it.kind.isAnterior }
        val posterior = teeth.isNotEmpty() && teeth.none { it.kind.isAnterior }
        val upper = teeth.isNotEmpty() && teeth.all { it.arch == Arch.MAXILLARY }
        val lower = teeth.isNotEmpty() && teeth.all { it.arch == Arch.MANDIBULAR }
        return when (this) {
            FACIAL -> when { anterior -> "Labial"; posterior -> "Buccal"; else -> "Facial" }
            LINGUAL -> when { upper -> "Palatal"; lower -> "Lingual"; else -> "Lingual/palatal" }
            MESIAL -> "Mesial"
            DISTAL -> "Distal"
            OCCLUSAL -> when { anterior -> "Incisal"; posterior -> "Occlusal"; else -> "Occlusal/incisal" }
        }
    }

    fun label(teeth: Collection<Tooth>, lang: app.morfo.i18n.AppLanguage): String =
        if (lang == app.morfo.i18n.AppLanguage.EN) labelEnTitle(teeth) else labelPl(teeth)
}
