package com.example.aitourism.controller;

import com.example.aitourism.ai.tool.POISearchTool;
import dev.langchain4j.service.tool.ToolProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/debug")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ToolDebugController {

    @Value("${ai.tool.mode:hybrid}")
    private String toolMode;

    @Value("${mcp.enabled:false}")
    private boolean mcpEnabled;

    @Value("${mcp.sse-url:}")
    private String mcpSseUrl;

    private final Optional<POISearchTool> poiSearchToolOptional;
    private final Optional<ToolProvider> mcpToolProviderOptional;

    @GetMapping("/tools")
    public Map<String, Object> tools() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("toolMode", toolMode);
        result.put("mcpEnabled", mcpEnabled);
        result.put("mcpSseUrlConfigured", mcpSseUrl != null && !mcpSseUrl.isBlank());
        result.put("localPoiToolBeanPresent", poiSearchToolOptional.isPresent());
        result.put("mcpToolProviderBeanPresent", mcpToolProviderOptional.isPresent());
        result.put("activeToolSources", resolveActiveToolSources());
        result.put("note", "mcpToolProviderBeanPresent=true 只表示 MCP 工具提供器已装配，不代表远端 MCP 服务已经可用");
        return result;
    }

    private String resolveActiveToolSources() {
        String mode = toolMode == null ? "hybrid" : toolMode.trim().toLowerCase();
        return switch (mode) {
            case "mcp" -> "mcp";
            case "local" -> "local";
            default -> "local + mcp";
        };
    }
}
