package com.barisdincer.circlekeep

import com.barisdincer.circlekeep.data.sortedByTurkish
import org.junit.Assert.assertEquals
import org.junit.Test

class TurkishTextSortTest {
    @Test
    fun `sorts names by Turkish alphabet order`() {
        val names = listOf("Ömer", "Can", "Çağla", "İrem", "Işıl", "Şule", "Umut", "Ümit")

        val sorted = names.sortedByTurkish { it }

        assertEquals(
            listOf("Can", "Çağla", "Işıl", "İrem", "Ömer", "Şule", "Umut", "Ümit"),
            sorted
        )
    }
}
