package com.embabel.example.travel.agent

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TravelPlanTest {

    @Nested
    inner class Mapping {

        @Test
        fun `one day only`() {
            val travelPlan = TravelPlan(
                title = "One day trip",
                plan = "Have a good time",
                days = listOf(
                    Day(LocalDate.of(2019, 12, 31), "Paris"),
                    Day(LocalDate.of(2020, 1, 1), "Dijon"),
                    Day(LocalDate.of(2020, 1, 2), "Dijon"),
                    Day(LocalDate.of(2020, 1, 3), "Beaune"),
                ),
                imageLinks = emptyList(),
                pageLinks = emptyList(),
            )
            println(travelPlan.journeyMap)
            println(travelPlan.journeyMapImage())
        }
    }

}