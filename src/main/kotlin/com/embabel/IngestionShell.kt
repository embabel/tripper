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
import com.embabel.agent.rag.*
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption

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


val schema = Schema(
    entities = listOf(
        EntityDefinition("Person", "A human being"),
        EntityDefinition("Organization", "A group of people working together"),
        EntityDefinition("Location", "A place"),
        EntityDefinition("Animal", "A living organism that is not a human or plant"),
    ),
    relationships = listOf(
        RelationshipDefinition(
            sourceEntity = "Person",
            targetEntity = "Organization",
            type = "works_at",
            description = "Indicates that a person works at an organization",
        ),
        RelationshipDefinition(
            sourceEntity = "Person",
            targetEntity = "Location",
            type = "lives_in",
            description = "Indicates that a person lives in a location",
        ),
        RelationshipDefinition(
            sourceEntity = "Person",
            targetEntity = "Animal",
            type = "has_pet",
            description = "Indicates that a person owns the specified animal as a pet",
        ),
        RelationshipDefinition(
            sourceEntity = "Person",
            targetEntity = "Person",
            type = "loves",
            description = "Indicates that a person loves the other person",
        ),
    ),
)


@ShellComponent("Ingestion commands")
internal class IngestionShell(
    private val knowledgeGraphBuilder: KnowledgeGraphBuilder,
    private val projector: Projector,
) {
    @ShellMethod
    fun input(
        @ShellOption(
            value = ["-r", "--resource"],
            defaultValue = "file:/Users/rjohnson/dev/embabel.com/travel-planner-agent/src/main/resources/data/rod.txt",
            help = "Path to the resource file to ingest",
        )
        resource: String,
    ): String {
//        val kgUpdate = knowledgeGraphBuilder.analyze(
//            resource, schema
//        )
        val kgUpdate = knowledgeGraphBuilder.computeUpdate(
            Chunk(
                id = "chunk-1",
                text = "Rod Johnson lives in Annandale with his girlfriend Lynda, and golden retriever Duke. Rod is CEO of Embabel.",
            ),
            schema,
        )
        println("Knowledge Graph Update:")
//        println(kgUpdate)
        println(kgUpdate.infoString(verbose = true))
        projector.project(kgUpdate)
        return "Ingestion complete"
    }

}
