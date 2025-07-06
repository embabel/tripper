package com.embabel.boogie

/**
 * Takes our knowledge graph update and projects it somewhere:
 * for example, to a graph database.
 */
interface KnowledgeGraphUpdater {

    /**
     * Project somewhere
     */
    fun applyDelta(
        knowledgeGraphDelta: KnowledgeGraphDelta,
    )
}
