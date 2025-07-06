package com.embabel.boogie.neo

import com.embabel.agent.rag.Chunk
import com.embabel.agent.rag.EntityData
import com.embabel.agent.rag.Retrievable
import com.embabel.boogie.*
import org.neo4j.ogm.session.Session
import org.neo4j.ogm.session.SessionFactory
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@ConfigurationProperties(prefix = "application.neo")
data class NeoOgmProjectorProperties(
    val chunkNodeName: String = "Document",
)

@Service
class NeoOgmProjector(
    private val sessionFactory: SessionFactory,
    private val properties: NeoOgmProjectorProperties,
) : Projector, SchemaSource, ChunkRepository {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun findAll(): List<Chunk> {
        val rows = sessionFactory.openSession().query(
            cypherChunkQuery(""),
            emptyMap<String, Any?>(),
            true,
        )
        return rows.map(::rowToChunk)

    }

    private fun cypherChunkQuery(whereClause: String): String =
        "MATCH (c:${properties.chunkNodeName}) $whereClause RETURN c.id AS id, c.text AS text, c.metadata.source as metadata_source"


    override fun findChunksById(chunkIds: List<String>): List<Chunk> {
        val rows = sessionFactory.openSession().query(
            cypherChunkQuery(" WHERE c.id IN \$ids "),
            mapOf("ids" to chunkIds),
            true,
        )
        return rows.map(::rowToChunk)
    }

    private fun rowToChunk(row: Map<String, Any?>): Chunk {
        val metadata = mutableMapOf<String, Any>()
        metadata["source"] = row["metadata_source"] ?: "unknown"
        return Chunk(
            id = row["id"] as String,
            text = row["text"] as String,
            metadata = metadata,
        )
    }

    override fun inferSchema(): KnowledgeGraphSchema {
        val metadata = sessionFactory.metaData()
        val relationships = mutableListOf<RelationshipDefinition>()
        val entityDefinitions = metadata.persistentEntities()
            .filter { it.hasPrimaryIndexField() }
            .map { entity ->
                val labels = entity.staticLabels().toSet()
                val entityDefinition = EntityDefinition(
                    labels = labels,
                    properties = emptyList(),
                    description = labels.joinToString(","),
                )
                entity.relationshipFields().forEach { relationshipField ->
                    val targetEntity = relationshipField.typeDescriptor.split(".").last()
                    relationships.add(
                        RelationshipDefinition(
                            sourceEntity = entityDefinition.type,
                            targetEntity = targetEntity,
                            type = relationshipField.relationship(),
                            description = relationshipField.name,
                            cardinality = if (relationshipField.isArray || relationshipField.isIterable) {
                                Cardinality.MANY
                            } else {
                                Cardinality.ONE
                            },
                        )
                    )
                }
                entityDefinition
            }
        return KnowledgeGraphSchema(
            entities = entityDefinitions,
            relationships = relationships,
        )
    }

    @Transactional
    override fun applyDelta(knowledgeGraphDelta: KnowledgeGraphDelta) {
        val session = sessionFactory.openSession()
        knowledgeGraphDelta.newEntities().forEach { ne ->
            createEntity(session, ne, knowledgeGraphDelta.basis)
        }
        knowledgeGraphDelta.mergedEntities().forEach { ne ->
            logger.warn("Ignoring merged entity: leaving it unchanged in the database: {}", ne)
        }
        knowledgeGraphDelta.newRelationships().forEach { relationship ->
            createRelationship(session, relationship.suggestedRelationship, knowledgeGraphDelta.basis)
        }
        knowledgeGraphDelta.mergedRelationships().forEach { relationship ->
//            createRelationship(session, relationship.suggestedRelationship, knowledgeGraphDelta.basis)
            logger.warn(
                "Ignoring merged relationship: leaving it unchanged in the database: {}",
                relationship.suggestedRelationship
            )
        }
    }

    private fun createEntity(
        session: Session,
        entity: EntityData,
        basis: Retrievable,
    ) {
        val entityCreationCypher = """
            MATCH (chunk:${properties.chunkNodeName} {id: ${'$'}basisId})
            CREATE (e:${entity.labels.joinToString(":")} {id: ${'$'}id, description: ${'$'}description })
                <-[:HAS_ENTITY]-(chunk) 
            SET e += ${'$'}properties
            RETURN e
            """.trimIndent()
        logger.info("Executing create entity cypher: {}", entityCreationCypher)
        session.query(
            entityCreationCypher,
            mapOf(
                "id" to entity.id,
                "description" to entity.description,
                "basisId" to basis.id,
                "properties" to entity.properties,
            )
        )
    }

    private fun createRelationship(
        session: Session,
        relationship: RelationshipInstance,
        basis: Retrievable,
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