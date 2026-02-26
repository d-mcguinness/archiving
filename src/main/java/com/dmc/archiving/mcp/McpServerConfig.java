package com.dmc.archiving.mcp;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration for MCP (Model Context Protocol) Servers
 * Only active in local profile for development assistance
 */
@Configuration
@Profile("local")
public class McpServerConfig {
    // Configuration for MCP servers when running in local profile
}

