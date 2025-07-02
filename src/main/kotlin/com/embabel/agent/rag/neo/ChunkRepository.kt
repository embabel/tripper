package com.embabel.agent.rag.neo

import com.embabel.agent.rag.Chunk

interface ChunkRepository {

    fun findChunksById(chunkIds: List<String>): List<Chunk>

    fun findAll(): List<Chunk>
}