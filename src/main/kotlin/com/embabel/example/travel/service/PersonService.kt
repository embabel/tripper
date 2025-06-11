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
package com.embabel.example.travel.service

import org.springframework.data.neo4j.repository.Neo4jRepository
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Repository
interface PersonRepository : Neo4jRepository<Person, Long> {
    fun findByName(name: String): Person?
}


@Service
@Transactional
class PersonService(
    private val personRepository: PersonRepository,
) {

    fun save(p: Person) {
        personRepository.save(p, 10)
    }

    fun loadPeople(): List<Person> {
        return personRepository.findAll().toList()
    }
}
