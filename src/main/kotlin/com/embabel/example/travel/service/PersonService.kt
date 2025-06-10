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