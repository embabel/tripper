package com.embabel.agent.rag

import org.neo4j.ogm.session.Session
import org.neo4j.ogm.session.SessionFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NeoOgmProjector(
    private val sessionFactory: SessionFactory,
) : Projector {

    @Transactional
    override fun project(knowledgeGraphUpdate: KnowledgeGraphUpdate) {
        val session = sessionFactory.openSession()
        knowledgeGraphUpdate.newEntities().forEach { entity ->
            createEntity(session, entity)
        }
        knowledgeGraphUpdate.relationships.forEach { relationship ->
            createRelationship(session, relationship)
        }
    }


    private fun createEntity(session: Session, entity: EntityData) {
        session.query(
            "CREATE (n:${entity.labels.joinToString(":")} {id: \$id, description: \$description })",
            mapOf("id" to entity.id, "description" to entity.description)
        )
    }

    private fun createRelationship(
        session: Session,
        relationship: SuggestedRelationship
    ) {
        session.query(
            """
                MATCH (n {id: ${'$'}sourceId}), (p {id: ${'$'}targetId}) 
                MERGE (n)-[:${relationship.type}]->(p)
               """.trimIndent(),
            mapOf(
                "sourceId" to relationship.sourceId,
                "targetId" to relationship.targetId
            )
        )
    }
}