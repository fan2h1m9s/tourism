package com.example.aitourism.config;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.service.tool.ToolProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Configuration
public class McpToolConfig {

    @Value("${mcp.enabled:false}")
    private boolean mcpEnabled;

    @Value("${mcp.sse-url:}")
    private String mcpSseUrl;

    @Value("${mcp.timeout-seconds:20}")
    private long timeoutSeconds;

    @Bean
    public Optional<ToolProvider> mcpToolProviderOptional() {
        if (!mcpEnabled || mcpSseUrl == null || mcpSseUrl.isBlank()) {
            return Optional.empty();
        }

        McpTransport transport = new HttpMcpTransport.Builder()
                .sseUrl(mcpSseUrl)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();

        McpClient mcpClient = new DefaultMcpClient.Builder()
                .transport(transport)
                .build();

        ToolProvider toolProvider = McpToolProvider.builder()
                .mcpClients(List.of(mcpClient))
                .build();

        return Optional.of(toolProvider);
    }
}
