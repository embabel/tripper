/*
 * Copyright 2024-2025 Embabel Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.embabel.example.travel.agent

import com.embabel.tripper.agent.Day
import com.embabel.tripper.agent.ProposedTravelPlan
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TravelPlanTest {

    @Nested
    inner class Mapping {

        @Test
        fun `one day only`() {
            val travelPlan = ProposedTravelPlan(
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
                countriesVisited = listOf("France"),
            )
//            println(travelPlan.plan.journeyMapUrl)
//            println(travelPlan.plan.journeyMapImageUrl())
        }
    }

}
