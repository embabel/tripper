package com.embabel.example.travel.service

import org.neo4j.ogm.annotation.GeneratedValue
import org.neo4j.ogm.annotation.Id
import org.neo4j.ogm.annotation.NodeEntity
import org.springframework.data.neo4j.repository.Neo4jRepository
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@NodeEntity
open class Person(
    var name: String,
) {
    @Id
    @GeneratedValue
    var id: Long? = null
}

@Repository
interface PersonRepository : Neo4jRepository<Person, String>


@Service
@Transactional
class PersonService(
    private val personRepository: PersonRepository,
) {

    init {
        personRepository.save(Person(name = "Alice"), 2)
    }

    fun save(p: Person) {
        personRepository.save(p, 10)
    }

    fun loadPeople(): List<Person> {
        personRepository.save(Person(name = "Bob2"), 2)
        return personRepository.findAll().toList()
    }
}