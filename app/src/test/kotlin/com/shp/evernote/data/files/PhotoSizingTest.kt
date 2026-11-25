package com.shp.evernote.data.files

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoSizingTest {
    @Test
    fun `small images are not sampled`() {
        assertEquals(1, sampleSizeFor(800, 600, 1600))
        assertEquals(1, sampleSizeFor(1600, 1200, 1600))
        assertEquals(1f, scaleFor(1600, 1200, 1600))
    }

    @Test
    fun `sample size is the largest power of two that keeps the long edge above the limit`() {
        // 4000 -> 2000 (sample 2) stays above 1600; 1000 would not.
        assertEquals(2, sampleSizeFor(4000, 3000, 1600))
        // 8000 -> 4000 -> 2000 (sample 4).
        assertEquals(4, sampleSizeFor(6000, 8000, 1600))
        // Exactly 3200 halves to 1600, which is allowed.
        assertEquals(2, sampleSizeFor(3200, 100, 1600))
    }

    @Test
    fun `scale brings the sampled long edge down to the limit`() {
        assertEquals(0.8f, scaleFor(2000, 1500, 1600), 1e-6f)
        assertEquals(0.5f, scaleFor(100, 3200, 1600), 1e-6f)
    }

    @Test
    fun `sampling then scaling never exceeds the limit`() {
        for ((w, h) in listOf(1 to 1, 1599 to 1601, 2047 to 2049, 4032 to 3024, 12000 to 9000)) {
            val s = sampleSizeFor(w, h, 1600)
            val sw = w / s
            val sh = h / s
            val f = scaleFor(sw, sh, 1600)
            val longEdge = maxOf((sw * f).toInt(), (sh * f).toInt())
            assert(longEdge <= 1600) { "$w x $h -> $longEdge" }
        }
    }
}
