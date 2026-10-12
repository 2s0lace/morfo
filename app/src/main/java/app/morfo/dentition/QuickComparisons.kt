package app.morfo.dentition

import app.morfo.i18n.AppLanguage

/** One tooth in a comparison set: FDI number plus geometry variant. */
data class ToothRef(val fdi: Int, val variant: ModelVariant = ModelVariant.STANDARD) {
    val tooth get() = Tooth(fdi)
}

data class QuickComparison(val titlePl: String, val titleEn: String, val teeth: List<ToothRef>) {
    fun title(lang: AppLanguage) = if (lang == AppLanguage.EN) titleEn else titlePl
}

/** Shortcuts only; the user can always build any combination manually. */
object QuickComparisons {
    private fun of(titlePl: String, titleEn: String, vararg fdi: Int) = QuickComparison(titlePl, titleEn, fdi.map { ToothRef(it) })

    val all = listOf(
        of("Górny siekacz przyśrodkowy vs boczny", "Maxillary central vs lateral incisor", 11, 12),
        of("Górny 1. vs 2. przedtrzonowiec", "Maxillary 1st vs 2nd premolar", 14, 15),
        of("Dolny 1. vs 2. przedtrzonowiec", "Mandibular 1st vs 2nd premolar", 44, 45),
        of("Górny 1. vs 2. trzonowiec", "Maxillary 1st vs 2nd molar", 16, 17),
        of("Dolny 1. vs 2. trzonowiec", "Mandibular 1st vs 2nd molar", 46, 47),
        of("Kieł górny vs dolny", "Maxillary vs mandibular canine", 13, 43),
        of("Dolny siekacz przyśrodkowy vs boczny", "Mandibular central vs lateral incisor", 41, 42),
        of("Przedtrzonowce górne vs dolne", "Maxillary vs mandibular premolars", 14, 15, 44, 45),
        of("Zęby przeciwstronne (14 vs 24)", "Contralateral teeth (14 vs 24)", 14, 24),
        QuickComparison(
            "Górny 1. trzonowiec: klasyczny vs z guzkiem Carabellego",
            "Maxillary 1st molar: standard vs with Carabelli's cusp",
            listOf(ToothRef(16), ToothRef(16, ModelVariant.CARABELLI)),
        ),
    )
}
