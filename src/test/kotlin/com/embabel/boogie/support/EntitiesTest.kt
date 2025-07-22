package com.embabel.boogie.support

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test

class EntitiesTest {

    private val jom = jacksonObjectMapper()

    @Test
    fun deserialize() {
        val json = """
            {
              "entities": [
                {
                  "type": "Person",
                  "name": "Rod",
                  "summary": "Rod is a person who has visited many countries, enjoys hobbies such as chess, music, cycling, and hiking, owns a golden retriever named Duke, works as CEO at Embabel, and lives in Annandale with his girlfriend Lynda."
                },
                {
                  "type": "Place",
                  "name": "France",
                  "summary": "France is a country that Rod has visited."
                },
                {
                  "type": "Activity",
                  "name": "chess",
                  "summary": "Chess is one of Rod's hobbies."
                },
                {
                  "type": "Activity",
                  "name": "music",
                  "summary": "Music, specifically playing the piano, is one of Rod's hobbies."
                },
                {
                  "type": "Activity",
                  "name": "cycling",
                  "summary": "Cycling is one of Rod's hobbies."
                },
                {
                  "type": "Activity",
                  "name": "hiking",
                  "summary": "Hiking is one of Rod's hobbies."
                },
                {
                  "type": "Dog,Animal",
                  "name": "Duke",
                  "summary": "Duke is a golden retriever owned by Rod."
                },
                {
                  "type": "Person",
                  "name": "Lynda",
                  "summary": "Lynda is Rod's girlfriend and lives with him in Annandale."
                },
                {
                  "type": "Place",
                  "name": "Annandale",
                  "summary": "Annandale is the place where Rod and Lynda live."
                }
              ]
            }
        """.trimIndent()
        val entities = jom.readValue(json, Entities::class.java)
    }

}