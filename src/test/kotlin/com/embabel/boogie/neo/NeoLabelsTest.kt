package com.embabel.boogie.neo

import com.embabel.agent.rag.SimpleNamedEntityData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NeoLabelsTest {

    @Test
    fun `one label`() {
        val duke = SimpleNamedEntityData(
            "duke", "Duke", "description",
            setOf("Dog"), emptyMap(),
        )
        assertEquals("Dog", duke.neoLabels())
    }

    @Test
    fun `two labels`() {
        val duke = SimpleNamedEntityData(
            "duke", "Duke", "description",
            setOf("Dog", "Animal"), emptyMap(),
        )
        assertEquals("Dog:Animal", duke.neoLabels())
    }

}