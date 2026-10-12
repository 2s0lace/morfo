package app.morfo.dentition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ToothTest {
    private val modelDir = File("src/main/assets/models")

    @Test
    fun everyPermanentToothResolvesToAnExistingModel() {
        for (tooth in Dentition.permanent) {
            for (variant in tooth.variants) {
                val file = File(modelDir, tooth.modelId(variant) + ".glb")
                assertTrue("${tooth.fdi} $variant → missing ${file.name}", file.exists())
            }
        }
    }

    @Test
    fun rightSideTeethMirrorTheirContralateralCounterpart() {
        assertEquals(24, Tooth(14).sourceFdi)
        assertEquals(35, Tooth(45).sourceFdi)
        assertEquals(26, Tooth(26).sourceFdi)
        assertEquals("t26_carabelli", Tooth(16).modelId(ModelVariant.CARABELLI))
        assertEquals("t36", Tooth(36).modelId(ModelVariant.CARABELLI)) // no variant for lower molars
    }

    @Test
    fun namesFollowArchSideAndKind() {
        assertEquals("Maxillary Right First Premolar", Tooth(14).nameEn)
        assertEquals("Górny prawy pierwszy przedtrzonowiec", Tooth(14).namePl)
        assertEquals("Dolny lewy kieł", Tooth(33).namePl)
    }
}
