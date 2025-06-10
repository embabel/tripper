package com.embabel.example.travel.service

import org.neo4j.ogm.annotation.GeneratedValue
import org.neo4j.ogm.annotation.Id
import org.neo4j.ogm.annotation.NodeEntity
import org.neo4j.ogm.annotation.Relationship

@NodeEntity
data class Person(
    val name: String,
    @Relationship(type = "ENJOYS", direction = Relationship.Direction.OUTGOING)
    val activities: List<Activity> = emptyList(),
    @Id
    @GeneratedValue
    val id: Long? = null,
)

@NodeEntity
data class Activity(
    val name: String,
    @Id
    @GeneratedValue
    val id: Long? = null,
)