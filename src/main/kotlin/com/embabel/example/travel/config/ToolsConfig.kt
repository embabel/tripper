package com.embabel.example.travel.config

import com.embabel.agent.core.ToolGroup
import com.embabel.agent.core.ToolGroupDescription
import com.embabel.agent.core.ToolGroupPermission
import com.embabel.agent.tools.mcp.McpToolGroup
import io.modelcontextprotocol.client.McpSyncClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ToolsConfig(
    private val mcpSyncClients: List<McpSyncClient>,
) {

    @Bean
    fun mcpAirbnbToolsGroup(): ToolGroup {
        return McpToolGroup(
            description = ToolGroupDescription(description = "Airbnb tools", role = AIRBNB),
            name = "openbnb-airbnb",
            provider = "Docker",
            permissions = setOf(
                ToolGroupPermission.INTERNET_ACCESS
            ),
            clients = mcpSyncClients,
            filter = {
                println(it.toolDefinition.name())
                it.toolDefinition.name().contains("airbnb")
            },
        )
    }

    companion object {
        private const val AIRBNB = "airbnb"
    }
}