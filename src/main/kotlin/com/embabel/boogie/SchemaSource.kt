package com.embabel.boogie

interface SchemaSource {

    /**
     * Infer the schema of the knowledge graph.
     */
    fun getSchema(name: String): KnowledgeGraphSchema
}