package app.morfo.workspace

import app.morfo.i18n.AppLanguage

enum class LabelMode(val labelPl: String, val labelEn: String) {
    FDI("Tylko FDI", "FDI only"),
    FDI_EN("FDI + nazwa angielska", "FDI + name"),
    FDI_PL("FDI + nazwa polska", "FDI + Polish name"),
    NONE("Bez etykiet", "No labels");

    fun label(lang: AppLanguage) = if (lang == AppLanguage.EN) labelEn else labelPl
}

enum class LightingMode(val labelPl: String, val descPl: String, val labelEn: String, val descEn: String) {
    CLINICAL("Kliniczne", "Zrównoważone światło, czytelna morfologia", "Clinical", "Balanced lighting, clear morphology"),
    HIGH_CONTRAST("Wysoki kontrast", "Mocne światło boczne, głębsze cienie", "High Contrast", "Strong side lighting, deeper shadows"),
    SOFT("Miękkie", "Rozproszone światło, łagodne przejścia", "Soft", "Diffuse lighting, gentle transitions"),
    MORPHOLOGY("Morfologia", "Światło skośne i kontaktowe cienie – uwydatnia bruzdy i guzki", "Morphology", "Oblique lighting and contact shadows – enhances fissures and cusps");

    val description get() = descPl
    fun label(lang: AppLanguage) = if (lang == AppLanguage.EN) labelEn else labelPl
    fun description(lang: AppLanguage) = if (lang == AppLanguage.EN) descEn else descPl
}

enum class Arrangement(val labelPl: String, val labelEn: String) {
    AUTO("Automatyczny", "Auto"),
    ROW("Rząd", "Row"),
    GRID("Siatka", "Grid"),
    TRIANGLE("Trójkąt", "Triangle");

    fun label(lang: AppLanguage) = if (lang == AppLanguage.EN) labelEn else labelPl
}

enum class ScaleMode(val labelPl: String, val descPl: String, val labelEn: String, val descEn: String) {
    ANATOMICAL("Anatomiczna", "Realne proporcje między zębami (średnie długości)", "Anatomical", "Real proportions between teeth (average lengths)"),
    UNIFORM("Jednakowa", "Każdy ząb tej samej wysokości – do oglądania szczegółów", "Uniform", "Every tooth scaled to equal height – for inspecting details");

    val description get() = descPl
    fun label(lang: AppLanguage) = if (lang == AppLanguage.EN) labelEn else labelPl
    fun description(lang: AppLanguage) = if (lang == AppLanguage.EN) descEn else descPl
}
