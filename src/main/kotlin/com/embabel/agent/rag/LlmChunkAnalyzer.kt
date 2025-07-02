package com.embabel.agent.rag

import com.embabel.agent.config.models.AnthropicModels
import com.embabel.agent.spi.InteractionId
import com.embabel.agent.spi.LlmInteraction
import com.embabel.agent.spi.LlmOperations
import com.embabel.common.ai.model.LlmOptions
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Service

@ConfigurationProperties("embabel.agent.rag")
data class ChunkAnalyzerProperties(
    var llm: String = AnthropicModels.CLAUDE_37_SONNET,
) {

    fun llmOptions(): LlmOptions = LlmOptions.fromModel(llm)
        .withTemperature(0.0)
}

@Service
class LlmChunkAnalyzer(
    private val llmOperations: LlmOperations,
    private val properties: ChunkAnalyzerProperties = ChunkAnalyzerProperties(),
) : ChunkAnalyzer {

    private val logger = LoggerFactory.getLogger(LlmChunkAnalyzer::class.java)

    override fun identifyEntities(chunk: Chunk, schema: Schema): SuggestedEntities {
        val prompt = """
            Given the following text, identify and summarize all entities mentioned.
            Include the entity id only if it's provided in the text as a UUID, not a name.
            Entity types must only come from the following list:
            
            ${schema.entities.joinToString("\n") { "- ${it.type}: ${it.description}" }}
            
            You must be sure that every entity is of one of the types above.
            For example, are you sure something is a company and not a location?
            
            # TEXT
            ${chunk.text}
        """.trimIndent()
        logger.info("Identifying entities with prompt:\n$prompt")
        val entities = llmOperations.doTransform(
            prompt,
            LlmInteraction(id = InteractionId("identify-entities"), llm = properties.llmOptions()),
            Entities::class.java,
            llmRequestEvent = null,
        )
        return SuggestedEntities(
            basis = chunk,
            suggestedEntities = entities.entities,
        )
    }

    override fun analyzeRelationships(
        entityResolution: SuggestedEntitiesResolution,
        schema: Schema
    ): KnowledgeGraphDelta {
        val entitiesToUse = entityResolution.resolutions.filterIsInstance<EntityDataResolution>().map { it.entityData }
        val prompt =
            """
            Given the following text, identify and summarize all relationships.
            Relationships must only come from the following list:
            
            ${
                schema.possibleRelationshipsBetween(entitiesToUse)
                    .joinToString("\n") { "(:${it.sourceEntity})-[:${it.type}]->(:${it.targetEntity}): cardinality=${it.cardinality}, ${it.description}" }
            }
            
            Relationships may only be between following entities:
            ${
                entitiesToUse
                    .joinToString("\n") { "- (:${it.labels.joinToString(":")} {id='${it.id}', description='${it.description}'})" }
            }
            
            # TEXT
            ${entityResolution.basis.infoString()}
        """.trimIndent()
        logger.info("Identifying relationships with prompt:\n$prompt")
        val relationships = llmOperations.doTransform(
            prompt,
            LlmInteraction(id = InteractionId("identify-relationships"), llm = properties.llmOptions()),
            Relationships::class.java,
            llmRequestEvent = null,
        )
        val allEntities = entityResolution.resolutions
            .filterIsInstance<EntityDataResolution>()
            .map { it.entityData }
        val newEntities = entityResolution.resolutions
            .filterIsInstance<NewEntity>()
            .map { it.entityData }
        val newRelationships = relationships.relationships
            .filter {
                val sourceEntity = allEntities.find { entity -> entity.id == it.sourceId }
                val targetEntity = allEntities.find { entity -> entity.id == it.targetId }
                if (sourceEntity == null || targetEntity == null) {
                    logger.warn("Internal error checking relationship: ${it.sourceId} -[${it.type}]-> ${it.targetId} because one of the entities is not found.")
                    return@filter false
                }

                it.isValid(schema, sourceEntity, targetEntity)
            }
        return KnowledgeGraphDelta(
            basis = entityResolution.basis,
            newEntities = newEntities,
            newRelationships = newRelationships,
        )
    }
}

private data class Entities(
    val entities: List<SuggestedEntity>,
)

private data class Relationships(
    val relationships: List<SuggestedRelationship>,
)