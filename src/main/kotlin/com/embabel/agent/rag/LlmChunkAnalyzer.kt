package com.embabel.agent.rag

import com.embabel.agent.spi.InteractionId
import com.embabel.agent.spi.LlmInteraction
import com.embabel.agent.spi.LlmOperations
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service


@Service
class LlmChunkAnalyzer(
    private val llmOperations: LlmOperations,
) : ChunkAnalyzer {

    private val logger = LoggerFactory.getLogger(LlmChunkAnalyzer::class.java)

    override fun identifyEntities(chunk: Chunk, schema: Schema): SuggestedEntities {
        val prompt = """
            Given the following text, identify and summarize all entities mentioned.
            Include the entity id only if it's provided in the text as a UUID, not a name.
            Entity types must only come from the following list:
            
            ${schema.entities.joinToString("\n") { "- ${it.type}: ${it.description}" }}
            
            # TEXT
            ${chunk.text}
        """.trimIndent()
        logger.info("Identifying entities with prompt:\n$prompt")
        val entities = llmOperations.doTransform(
            prompt,
            LlmInteraction(id = InteractionId("identify-entities")),
            Entities::class.java,
            llmRequestEvent = null,
        )
        return SuggestedEntities(
            chunk = chunk,
            suggestedEntities = entities.entities,
        )
    }

    override fun analyzeRelationships(entityResolution: EntityResolution): KnowledgeGraphUpdate {
        val prompt = """
            Given the following text, identify and summarize all relationships.
            Relationships must only come from the following list:
            
            ${
            schema.possibleRelationshipsBetween(entityResolution.resolvedEntities.mapNotNull { it.entityData })
                .joinToString("\n") { "(:${it.sourceEntity})-[:${it.type}]->(:${it.targetEntity}): ${it.description}" }
        }
            
            Use entity IDs from the following list:
            ${
            entityResolution.resolvedEntities.mapNotNull { it.entityData }
                .joinToString("\n") { "- (:${it.labels.joinToString(":")} {id='${it.id}', description='${it.description}'})" }
        }
            
            # TEXT
            ${entityResolution.chunk.text}
        """.trimIndent()
        logger.info("Identifying relationships with prompt:\n$prompt")
        val relationships = llmOperations.doTransform(
            prompt,
            LlmInteraction(id = InteractionId("identify-relationships")),
            Relationships::class.java,
            llmRequestEvent = null,
        )
        // TODO filter out relationships that don't match the schema
        return KnowledgeGraphUpdate(
            entityResolution = entityResolution,
            relationships = relationships.relationships,
        )
    }
}

private data class Entities(
    val entities: List<SuggestedEntity>,
)

private data class Relationships(
    val relationships: List<SuggestedRelationship>,
)