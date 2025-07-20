package com.embabel.boogie.shell

import com.embabel.boogie.KnowledgeGraphBuilder
import com.embabel.boogie.KnowledgeGraphUpdater
import com.embabel.boogie.SchemaSource
import com.embabel.boogie.neo.ChunkRepository
import com.embabel.tripper.service.PersonService
import org.slf4j.LoggerFactory
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod

@ShellComponent("Ingestion commands")
internal class IngestionShell(
    private val knowledgeGraphBuilder: KnowledgeGraphBuilder,
    private val knowledgeGraphUpdater: KnowledgeGraphUpdater,
    private val schemaSource: SchemaSource,
    private val personService: PersonService,
    private val chunkRepository: ChunkRepository,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    val schema = run {
        val schema = schemaSource.getSchema("default")
        logger.info("Using schema: {}", schema.infoString(verbose = true))
        schema
    }

    @ShellMethod
    fun list(): String {
        val people = personService.loadPeople()
        return people.joinToString("\n") { it.toString() }
    }

    // Use generic ingest first

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
        knowledgeGraphUpdater.applyDelta(kgDelta)
        return "Ingestion complete"
    }

}