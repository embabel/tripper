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
import org.neo4j.ogm.annotation.Relationship

interface NameAsId {

    val id: String?

    /**
     * The name of the entity, which is used as the ID.
     * This is a convenience for entities that do not have a separate ID field.
     */
    val name: String
        get() = id ?: "Anon"
}

@NodeEntity
data class Person(
    @Relationship(type = "ENJOYS", direction = Relationship.Direction.OUTGOING)
    val activities: List<Activity> = emptyList(),
    @Relationship(type = "VISITED", direction = Relationship.Direction.OUTGOING)
    val visit: List<Visit> = emptyList(),
    @Id
    override val id: String? = null,
) : NameAsId

@NodeEntity
data class Activity(
    @Id
    override val id: String? = null,
) : NameAsId

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
    override val id: String? = null,
) : NameAsId

@NodeEntity
data class Place(
    @Id
    override val id: String? = null,
) : NameAsId
