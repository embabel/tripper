package com.embabel.example.travel.config

import com.embabel.agent.rag.RagService
import com.embabel.agent.rag.support.SpringVectorStoreRagService
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RagConfiguration {

    @Bean
    fun ragService(vectorStore: VectorStore): RagService {
        return SpringVectorStoreRagService(
            vectorStore = vectorStore,
            description = "Travel Example Rag Service"
        )
    }
}