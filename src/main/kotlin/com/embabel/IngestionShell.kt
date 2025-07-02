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
package com.embabel

import com.embabel.agent.config.annotation.*
import com.embabel.agent.rag.KnowledgeGraphBuilder
import com.embabel.agent.rag.Projector
import com.embabel.agent.rag.SchemaSource
import com.embabel.agent.rag.neo.ChunkRepository
import com.embabel.tripper.service.PersonService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAgents(
    loggingTheme = LoggingThemes.SEVERANCE,
    localModels = [LocalModels.DOCKER, LocalModels.OLLAMA],
    mcpServers = [McpServers.DOCKER, McpServers.DOCKER_DESKTOP],
)
@EnableAgentShell
class IngestionApplication

fun main(args: Array<String>) {
    runApplication<IngestionApplication>(*args)
}


@ShellComponent("Ingestion commands")
internal class IngestionShell(
    private val knowledgeGraphBuilder: KnowledgeGraphBuilder,
    private val projector: Projector,
    private val schemaSource: SchemaSource,
    private val personService: PersonService,
    private val chunkRepository: ChunkRepository,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    val schema = run {
        val schema = schemaSource.inferSchema()
        logger.info("Using schema: {}", schema.infoString(verbose = true))
        schema
    }

    @ShellMethod
    fun list(): String {
        val people = personService.loadPeople()
        return people.joinToString("\n") { it.toString() }
    }

    // Use ingest first

    @ShellMethod
    fun analyze(
//        @ShellOption(
//            value = ["-r", "--resource"],
//            defaultValue = "file:/Users/rjohnson/dev/embabel.com/travel-planner-agent/src/main/resources/data/rod.txt",
//            help = "Path to the resource file to ingest",
//        )
//        resource: String,
    ): String {

        val chunks = chunkRepository.findAll()
        logger.info("Analyzing chunks: {}", chunks)

        val kgDelta = knowledgeGraphBuilder.computeDelta(
            chunks,
            schema,
        )
//        val kgUpdate = knowledgeGraphBuilder.computeUpdate(
//            Chunk(
//                id = "chunk-1",
//                text = "Rod Johnson lives in Annandale with his girlfriend Lynda, and golden retriever Duke. Rod is CEO of Embabel.",
//            ),
//            schema,
//        )
        if (kgDelta == null) {
            logger.warn("No knowledge graph update computed")
            return "No knowledge graph update computed"
        }
        logger.info("Knowledge graph delta:\n{}", kgDelta.infoString(verbose = true))
        projector.applyDelta(kgDelta)
        return "Ingestion complete"
    }

}
