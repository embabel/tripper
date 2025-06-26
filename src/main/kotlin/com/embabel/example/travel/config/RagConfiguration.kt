/*
 * Copyright 2024-2025 Embabel Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.embabel.example.travel.config

import com.embabel.agent.rag.RagService
import com.embabel.agent.rag.support.SpringVectorStoreRagService
import com.embabel.common.ai.model.ModelProvider
import com.embabel.common.ai.model.ModelSelectionCriteria
import org.neo4j.driver.Driver
import org.springframework.ai.transformer.splitter.TextSplitter
import org.springframework.ai.transformer.splitter.TokenTextSplitter
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.ai.vectorstore.neo4j.Neo4jVectorStore
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

    @Bean
    fun vectorStore(driver: Driver, modelProvider: ModelProvider): VectorStore {
        val embeddingService = modelProvider.getEmbeddingService(ModelSelectionCriteria.Auto).model

        return Neo4jVectorStore.builder(
            driver,
            embeddingService,
        )
            .initializeSchema(true)
            .embeddingDimension(384)
            .build()
    }

    @Bean
    fun textSplitter(): TextSplitter {
        return TokenTextSplitter()
    }
}
