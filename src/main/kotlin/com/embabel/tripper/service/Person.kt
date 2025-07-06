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
package com.embabel.tripper.service

import org.neo4j.ogm.annotation.Id
import org.neo4j.ogm.annotation.NodeEntity
import org.neo4j.ogm.annotation.Property
import org.neo4j.ogm.annotation.Relationship


@NodeEntity
data class Person(
    @Property
    val name: String,
    @Relationship(type = "ENJOYS", direction = Relationship.Direction.OUTGOING)
    val activities: List<Activity> = emptyList(),
    @Relationship(type = "VISITED", direction = Relationship.Direction.OUTGOING)
    val visit: List<Visit> = emptyList(),
    @Relationship(type = "LIVES_IN", direction = Relationship.Direction.OUTGOING)
    val place: Place? = null,
    @Relationship(type = "LOVES", direction = Relationship.Direction.UNDIRECTED)
    val partner: Person? = null,
    @Relationship(type = "OWNS_PET", direction = Relationship.Direction.OUTGOING)
    val pets: List<Animal> = emptyList(),
    @Id
    val id: String? = null,
)

@NodeEntity
data class Activity(
    @Property
    val name: String,
    @Id
    val id: String? = null,
)

/**
 * @param place The location of the visit. Must be nullable to handle depth.
 * @param rating The rating of the visit, from 1 to 5.
 */
@NodeEntity
data class Visit(
    @Relationship(type = "TO_PLACE", direction = Relationship.Direction.OUTGOING)
    val place: Place? = null,
    val rating: Int,
    val comment: String? = null,
    val year: Int? = null,
    @Id
    val id: String? = null,
)

@NodeEntity
data class Place(
    @Property
    val name: String,
    @Id
    val id: String? = null,
)

// TODO inheritance is not working yet in relationship determination
@NodeEntity
open class Animal(
    @Property
    val name: String,
    @Id
    val id: String? = null,
)

@NodeEntity
class Dog(
    name: String,
    val breed: String? = null,
    id: String? = null,
) : Animal(name = name, id = id)
