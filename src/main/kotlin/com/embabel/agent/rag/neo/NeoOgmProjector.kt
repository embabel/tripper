package com.embabel.agent.rag.neo

import com.embabel.agent.rag.*
import org.neo4j.ogm.session.Session
import org.neo4j.ogm.session.SessionFactory
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NeoOgmProjector(
    private val sessionFactory: SessionFactory,
) : Projector, SchemaSource {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun inferSchema(): Schema {
        val metadata = sessionFactory.metaData()
        val relationships = mutableListOf<RelationshipDefinition>()
        val entityDefinitions = metadata.persistentEntities()
            .map { entity ->
                val entityDefinition = EntityDefinition(
                    description = entity.neo4jName(),
                    labels = entity.staticLabels().toSet(),
                    properties = emptyList(),
                )
                entity.relationshipFields().forEach { relationshipField ->
                    // TODO this is wrong, look up labels
                    val targetEntity = relationshipField.field.type.simpleName
                    relationships.add(
                        RelationshipDefinition(
                            sourceEntity = entityDefinition.type,
                            targetEntity = targetEntity,
                            type = relationshipField.relationship(),
                            description = relationshipField.name,
                        )
                    )
                }
                entityDefinition
            }
        return Schema(
            entities = entityDefinitions,
            relationships = relationships,
        )
    }

    @Transactional
    override fun applyDelta(knowledgeGraphDelta: KnowledgeGraphDelta) {
        val session = sessionFactory.openSession()
        knowledgeGraphDelta.entityResolution.resolvedEntities.filterIsInstance<NewEntity>().forEach { ne ->
            createEntity(session, ne.entityData)
        }
        knowledgeGraphDelta.relationships.forEach { relationship ->
            createRelationship(session, relationship)
        }
    }


    private fun createEntity(session: Session, entity: EntityData) {
        val cypher = "CREATE (n:${entity.labels.joinToString(":")} {id: \$id, description: \$description })"
        logger.info("Executing create entity cypher: {}", cypher)
        session.query(
            cypher,
            mapOf("id" to entity.id, "description" to entity.description)
        )
    }

    private fun createRelationship(
        session: Session,
        relationship: SuggestedRelationship
    ) {
        val cypher = """
                MATCH (n {id: ${'$'}sourceId}), (p {id: ${'$'}targetId}) 
                MERGE (n)-[:${relationship.type}]->(p)
               """.trimIndent()
        logger.info("Executing create relationship cypher: {}", cypher)
        session.query(
            cypher,
            mapOf(
                "sourceId" to relationship.sourceId,
                "targetId" to relationship.targetId
            )
        )
    }
}