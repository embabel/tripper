package com.embabel.boogie

/**
 * Takes our knowledge graph update and projects it somewhere:
 * for example, to a graph database.
 */
interface KnowledgeGraphUpdater {

    /**
     * Apply this delta to the knowledge graph.
     */
    fun applyDelta(
        knowledgeGraphDelta: KnowledgeGraphDelta,
    )
}
