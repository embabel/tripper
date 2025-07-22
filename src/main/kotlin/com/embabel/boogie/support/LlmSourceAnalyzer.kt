package com.embabel.boogie.support

import com.embabel.agent.config.models.AnthropicModels
import com.embabel.agent.rag.Chunk
import com.embabel.agent.spi.InteractionId
import com.embabel.agent.spi.LlmInteraction
import com.embabel.agent.spi.LlmOperations
import com.embabel.boogie.*
import com.embabel.boogie.schema.KnowledgeGraphSchema
import com.embabel.common.ai.model.LlmOptions
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Service

@ConfigurationProperties("embabel.boogie.llm-source-analyzer")
data class LlmSourceAnalyzerProperties(
    val llm: String = AnthropicModels.CLAUDE_37_SONNET,
    val temperature: Double = 0.0,
) {

    fun llmOptions(): LlmOptions = LlmOptions.fromModel(llm)
        .withTemperature(temperature)
}

@Service
class LlmSourceAnalyzer(
    private val llmOperations: LlmOperations,
    private val properties: LlmSourceAnalyzerProperties = LlmSourceAnalyzerProperties(),
) : SourceAnalyzer {

    private val logger = LoggerFactory.getLogger(LlmSourceAnalyzer::class.java)

    override fun suggestEntities(chunk: Chunk, schema: KnowledgeGraphSchema): SuggestedEntities {
        val prompt = """
            Given the following text, identify and summarize all entities mentioned.
            Include the entity id only if it's provided in the text as a UUID, not a name.
            Entity types must only come from the following list:
            
            ${schema.entities.joinToString("\n") { "- ${it.type}: ${it.description}" }}
            
            IMPORTANT: You must find every entity of any of these types. Do not skip any.
            
            You must be sure that every entity is of one of the types above.
            For example, are you sure something is a company and not a location?
            
            # TEXT
            ${chunk.text}
        """.trimIndent()
        logger.info("Identifying entities using prompt:\n$prompt")
        val entities = llmOperations.doTransform(
            prompt,
            LlmInteraction(
                id = InteractionId("identify-entities"),
                llm = properties.llmOptions(),
            ),
            Entities::class.java,
            llmRequestEvent = null,
        )
        return SuggestedEntities(
            basis = chunk,
            suggestedEntities = entities.entities,
        )
    }

    override fun suggestRelationships(
        suggestedEntitiesResolution: SuggestedEntitiesResolution,
        schema: KnowledgeGraphSchema
    ): SuggestedRelationships {
        val entitiesToUse =
            suggestedEntitiesResolution.resolutions.filterIsInstance<EntityDataResolution>().map { it.kgEntity }
        val prompt =
            """
            Given the following text, identify and summarize all relationships.
            Relationships must only come from the following list:
            
            ${
                schema.possibleRelationshipsBetween(entitiesToUse)
                    .joinToString("\n") { "(:${it.sourceLabel})-[:${it.type}]->(:${it.targetLabel}): cardinality=${it.cardinality}, ${it.description}" }
            }
            
            Relationships may only be between following entities:
            ${
                entitiesToUse
                    .joinToString("\n") { "- (:${it.labels.joinToString(":")} {id='${it.id}', name='${it.name}', description='${it.description}'})" }
            }
            
            IMPORTANT: You must find all relationships implied in the text between any of these types.
            Do not skip any.
            
            # TEXT
            ${suggestedEntitiesResolution.basis.infoString()}
        """.trimIndent()
        logger.info("Identifying relationships with prompt:\n$prompt")
        val relationships = llmOperations.doTransform(
            prompt,
            LlmInteraction(id = InteractionId("identify-relationships"), llm = properties.llmOptions()),
            Relationships::class.java,
            llmRequestEvent = null,
        )
        val allEntities = suggestedEntitiesResolution.resolutions
            .filterIsInstance<EntityDataResolution>()
            .map { it.kgEntity }
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
        return SuggestedRelationships(
            entitiesResolution = suggestedEntitiesResolution,
            suggestedRelationships = newRelationships,
        )
    }
}

internal data class Entities(
    val entities: List<SuggestedEntity>,
)

private data class Relationships(
    val relationships: List<SuggestedRelationship>,
)