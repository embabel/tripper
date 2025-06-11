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
package com.embabel.example.travel

import org.neo4j.ogm.session.SessionFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableNeo4jRepositories
@EnableTransactionManagement
class NeoOgmConfig {

    @Value("\${spring.data.neo4j.uri}")
    private lateinit var uri: String

    @Value("\${spring.data.neo4j.username}")
    private lateinit var username: String

    @Value("\${spring.data.neo4j.password}")
    private lateinit var password: String

    @Bean
    fun configuration(): org.neo4j.ogm.config.Configuration {
        return org.neo4j.ogm.config.Configuration.Builder()
            .uri(uri)
            .credentials(username, password)
            .build()
    }

    @Bean
    fun sessionFactory(): SessionFactory {
        return SessionFactory(configuration(), "com.embabel.example.travel")
    }

    @Bean
    @Primary
    fun transactionManager(): PlatformTransactionManager {
        return Neo4jTransactionManager(sessionFactory())
    }
}
