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
package com.embabel.agent.rag

import com.embabel.agent.config.annotation.*
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
    ),
)


@ShellComponent("Ingestion commands")
internal class IngestionShell(
    private val chunkIngester: ChunkIngester,
    private val projector: Projector,
) {
    @ShellMethod
    fun input(
    ): String {
        val kgUpdate = chunkIngester.computeUpdate(
            Chunk(
                text = """
                    Rod owns a golden retriever named Duke.
                    Rod works at Embabel, where he is CEO.
                    Rod's girlfriend is Lynda.
                    Rod and Lynda live in Annandale.
                """.trimIndent(),
            ), schema
        )
//        println(kgUpdate)
        println(kgUpdate.infoString(verbose = true))
        projector.project(kgUpdate)
        return "Ingestion complete"
    }

}
