package com.embabel

import com.embabel.boogie.EntityDefinition
import com.embabel.boogie.KnowledgeGraphSchema
import com.embabel.boogie.RelationshipDefinition

val PERSON_SCHEMA = KnowledgeGraphSchema(
    entities = listOf(
        EntityDefinition("Person", "A human being"),
        EntityDefinition("Organization", "A group of people working together"),
        EntityDefinition("Location", "A place"),
        EntityDefinition("Animal", "A living organism that is not a human or plant"),
    ),
    relationships = listOf(
        RelationshipDefinition(
            sourceEntity = "Person",
            targetEntity = "Organization",
            type = "works_at",
            description = "Indicates that a person works at an organization",
        ),
        RelationshipDefinition(
            sourceEntity = "Person",
            targetEntity = "Location",
            type = "lives_in",
            description = "Indicates that a person lives in a location",
        ),
        RelationshipDefinition(
            sourceEntity = "Person",
            targetEntity = "Animal",
            type = "has_pet",
            description = "Indicates that a person owns the specified animal as a pet",
        ),
        RelationshipDefinition(
            sourceEntity = "Person",
            targetEntity = "Person",
            type = "loves",
            description = "Indicates that a person loves the other person",
        ),
    ),
)