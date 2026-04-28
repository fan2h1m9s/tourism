package com.example.aitourism.config;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.service.tool.ToolProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.util.List;

@Configuration
public class McpToolConfig {

    @Value("${mcp.timeout-seconds:20}")
    private long timeoutSeconds;

    private static final Logger log = LoggerFactory.getLogger(McpToolConfig.class);

    @Bean
    @ConditionalOnExpression("${mcp.enabled:false} && !'${mcp.url:}'.endsWith('key=') && !'${mcp.url:}'.isBlank()")
    public ToolProvider mcpToolProvider(@Value("${mcp.url}") String mcpUrl) {
        log.info("正在初始化 MCP 客户端: url={}", mcpUrl);

        McpTransport transport = StreamableHttpMcpTransport.builder()
                .url(mcpUrl)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();

        McpClient mcpClient = new DefaultMcpClient.Builder()
                .transport(transport)
                .build();

        ToolProvider toolProvider = McpToolProvider.builder()
                .mcpClients(List.of(mcpClient))
                .build();

        log.info("MCP ToolProvider 初始化成功");
        return toolProvider;
    }
}
