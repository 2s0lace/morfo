package app.morfo.dentition

import app.morfo.i18n.AppLanguage

enum class Arch(val labelPl: String, val labelEn: String) {
    MAXILLARY("Szczęka", "Maxillary"),
    MANDIBULAR("Żuchwa", "Mandibular");

    fun label(lang: AppLanguage) = if (lang == AppLanguage.EN) labelEn else labelPl
}

/** Patient's side. */
enum class Side(val labelPl: String, val labelEn: String) {
    RIGHT("Prawa", "Right"),
    LEFT("Lewa", "Left");

    fun label(lang: AppLanguage) = if (lang == AppLanguage.EN) labelEn else labelPl
}

enum class ToothClass(val labelPl: String, val labelEn: String) {
    INCISOR("Siekacze", "Incisors"),
    CANINE("Kły", "Canines"),
    PREMOLAR("Przedtrzonowce", "Premolars"),
    MOLAR("Trzonowce", "Molars");

    fun label(lang: AppLanguage) = if (lang == AppLanguage.EN) labelEn else labelPl
}

/** Position within a quadrant (the second FDI digit). */
enum class ToothKind(val position: Int, val toothClass: ToothClass, val nameEn: String, val namePl: String) {
    CENTRAL_INCISOR(1, ToothClass.INCISOR, "Central Incisor", "siekacz przyśrodkowy"),
    LATERAL_INCISOR(2, ToothClass.INCISOR, "Lateral Incisor", "siekacz boczny"),
    CANINE(3, ToothClass.CANINE, "Canine", "kieł"),
    FIRST_PREMOLAR(4, ToothClass.PREMOLAR, "First Premolar", "pierwszy przedtrzonowiec"),
    SECOND_PREMOLAR(5, ToothClass.PREMOLAR, "Second Premolar", "drugi przedtrzonowiec"),
    FIRST_MOLAR(6, ToothClass.MOLAR, "First Molar", "pierwszy trzonowiec"),
    SECOND_MOLAR(7, ToothClass.MOLAR, "Second Molar", "drugi trzonowiec"),
    THIRD_MOLAR(8, ToothClass.MOLAR, "Third Molar", "trzeci trzonowiec");

    val isAnterior get() = toothClass == ToothClass.INCISOR || toothClass == ToothClass.CANINE

    fun name(lang: AppLanguage) = if (lang == AppLanguage.EN) nameEn else namePl

    companion object {
        fun of(position: Int) = entries.first { it.position == position }
    }
}

/** Geometry variant of a tooth. Only the maxillary first molar has an alternative specimen. */
enum class ModelVariant(val suffix: String, val labelPl: String, val labelEn: String) {
    STANDARD("", "Klasyczny", "Standard"),
    CARABELLI("_carabelli", "Z guzkiem Carabellego", "With cusp of Carabelli");

    fun label(lang: AppLanguage) = if (lang == AppLanguage.EN) labelEn else labelPl
}

/** A permanent tooth identified by its FDI number (11–48). Pure metadata, no geometry. */
data class Tooth(val fdi: Int) {
    init { require(fdi / 10 in 1..4 && fdi % 10 in 1..8) { "Invalid permanent FDI number $fdi" } }

    val quadrant get() = fdi / 10
    val kind get() = ToothKind.of(fdi % 10)
    val toothClass get() = kind.toothClass
    val arch get() = if (quadrant <= 2) Arch.MAXILLARY else Arch.MANDIBULAR
    val side get() = if (quadrant == 1 || quadrant == 4) Side.RIGHT else Side.LEFT

    /** Only left-side teeth have source scans; right-side teeth are rendered as their mirror image. */
    val isMirrored get() = side == Side.RIGHT
    val sourceFdi: Int
        get() = when (quadrant) {
            1 -> fdi + 10 // 1x → 2x
            4 -> fdi - 10 // 4x → 3x
            else -> fdi
        }

    val nameEn get() = "${arch.labelEn} ${side.labelEn} ${kind.nameEn}"
    val namePl: String
        get() {
            val archPl = if (arch == Arch.MAXILLARY) "Górny" else "Dolny"
            val sidePl = if (side == Side.RIGHT) "prawy" else "lewy"
            return "$archPl $sidePl ${kind.namePl}"
        }

    fun name(lang: AppLanguage) = if (lang == AppLanguage.EN) nameEn else namePl

    val variants: List<ModelVariant>
        get() = if (arch == Arch.MAXILLARY && kind == ToothKind.FIRST_MOLAR) ModelVariant.entries else listOf(ModelVariant.STANDARD)

    /** Runtime asset id produced by tools/asset-pipeline, e.g. "t24" or "t26_carabelli". */
    fun modelId(variant: ModelVariant = ModelVariant.STANDARD) =
        "t$sourceFdi" + if (variant in variants) variant.suffix else ""

    /** Average overall length (crown + root) in mm used to scale the model, per Wheeler's tables. */
    val averageLengthMm: Float
        get() = when (arch) {
            Arch.MAXILLARY -> floatArrayOf(23.5f, 22.0f, 27.0f, 22.5f, 22.5f, 20.5f, 19.0f, 17.5f)
            Arch.MANDIBULAR -> floatArrayOf(21.5f, 23.5f, 27.0f, 22.5f, 22.5f, 21.5f, 20.0f, 18.0f)
        }[kind.position - 1]

    override fun toString() = fdi.toString()
}

object Dentition {
    /** All 32 permanent teeth in FDI order. */
    val permanent: List<Tooth> = (1..4).flatMap { q -> (1..8).map { Tooth(q * 10 + it) } }

    fun tooth(fdi: Int) = Tooth(fdi)

    /** Chart rows as seen by the clinician facing the patient: patient's right on the left. */
    val upperRow: List<Tooth> = (18 downTo 11).map(::Tooth) + (21..28).map(::Tooth)
    val lowerRow: List<Tooth> = (48 downTo 41).map(::Tooth) + (31..38).map(::Tooth)
}
