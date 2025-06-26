package com.embabel.example.travel

import org.neo4j.ogm.session.SessionFactory
import org.slf4j.LoggerFactory
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
class NeoOgmConfig(
    @Value("\${spring.neo4j.uri}")
    private val uri: String,

    @Value("\${spring.neo4j.authentication.username}")
    private val username: String,

    @Value("\${spring.neo4j.authentication.password}")
    private val password: String,
) {

    private val logger = LoggerFactory.getLogger(NeoOgmConfig::class.java)

    @Bean
    fun configuration(): org.neo4j.ogm.config.Configuration {
        logger.info("Connecting to Neo4j at {} as user {}", uri, username)
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