package com.embabel.agent.rag

/**
 * Takes our knowledge graph update and projects it somewhere:
 * for example, to a graph database.
 */
interface Projector {

    /**
     * Project somewhere
     */
    fun applyDelta(
        knowledgeGraphDelta: KnowledgeGraphDelta,
    )
}