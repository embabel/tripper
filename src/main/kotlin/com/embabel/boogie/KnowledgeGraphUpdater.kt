package com.embabel.boogie

/**
 * Takes our knowledge graph update and projects it somewhere:
 * for example, to a graph database.
 */
interface KnowledgeGraphUpdater {

    /**
     * Apply this delta to the knowledge graph.
     * Responsible for updating embeddings
     */
    fun applyDelta(
        knowledgeGraphDelta: KnowledgeGraphDelta,
    )
}

