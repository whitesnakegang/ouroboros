package kr.co.ouroboros.core.websocket.spec.service;

import kr.co.ouroboros.core.global.spec.SpecValidationUtil;
import kr.co.ouroboros.core.websocket.common.yaml.WebSocketYamlParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Manager for WebSocket server lifecycle management.
 * <p>
 * Handles server creation, validation, and extraction of server properties.
 * Servers are automatically created when operations reference them.
 *
 * @since 0.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketServerManager {

    private final WebSocketYamlParser yamlParser;

    /**
     * Ensures a server entry point exists, creating it if necessary.
     * <p>
     * Server name is generated from protocol and pathname (e.g., "ws-ws", "wss-stomp_v1").
     * Host is automatically set to "localhost:8080".
     * If a server with the same protocol and pathname already exists, reuses it.
     *
     * @param asyncApiDoc AsyncAPI document
     * @param protocol WebSocket protocol (ws or wss)
     * @param pathname WebSocket pathname (entry point)
     */
    public void ensureServerExists(Map<String, Object> asyncApiDoc, String protocol, String pathname) {
        // Validate pathname does not contain Korean characters
        SpecValidationUtil.validateNoKorean(pathname, "Pathname");

        // Generate server name from protocol + pathname
        String serverName = generateServerName(protocol, pathname);

        // Check if server already exists
        Map<String, Object> existingServer = yamlParser.getServer(asyncApiDoc, serverName);
        if (existingServer != null) {
            // Server already exists, no action needed
            log.debug("Reusing existing server: {} (protocol: {}, pathname: {})", serverName, protocol, pathname);
            return;
        }

        // Create new server
        Map<String, Object> serverDefinition = new java.util.LinkedHashMap<>();
        serverDefinition.put("host", "localhost:8080");
        serverDefinition.put("pathname", pathname);
        serverDefinition.put("protocol", protocol);
        serverDefinition.put("description", protocol.toUpperCase() + " WebSocket server at " + pathname);

        yamlParser.putServer(asyncApiDoc, serverName, serverDefinition);
        log.debug("Auto-created server: {} (protocol: {}, pathname: {})", serverName, protocol, pathname);
    }

    /**
     * Generates a server name from protocol and pathname.
     * <p>
     * Pattern: protocol-pathname_sanitized (e.g., "ws-ws", "wss-stomp_v1")
     *
     * @param protocol WebSocket protocol (ws or wss)
     * @param pathname WebSocket pathname
     * @return sanitized server name
     */
    public String generateServerName(String protocol, String pathname) {
        // Sanitize pathname: remove leading slash, replace remaining slashes with underscores
        String sanitizedPathname = pathname.startsWith("/") ? pathname.substring(1) : pathname;
        sanitizedPathname = sanitizedPathname.replace("/", "_");

        return protocol + "-" + sanitizedPathname;
    }

    /**
     * Extracts protocol from existing server in the document.
     * <p>
     * Looks for the first server and returns its protocol.
     *
     * @param asyncApiDoc AsyncAPI document
     * @return protocol (ws or wss), or null if not found
     */
    @SuppressWarnings("unchecked")
    public String extractProtocol(Map<String, Object> asyncApiDoc) {
        Map<String, Object> servers = yamlParser.getServers(asyncApiDoc);
        if (servers == null || servers.isEmpty()) {
            return null;
        }

        // Get first server
        Map<String, Object> firstServer = servers.values().stream()
                .filter(server -> server instanceof Map)
                .map(server -> (Map<String, Object>) server)
                .findFirst()
                .orElse(null);

        if (firstServer != null) {
            return (String) firstServer.get("protocol");
        }

        return null;
    }

    /**
     * Extracts pathname from existing server in the document.
     * <p>
     * Looks for the first server and returns its pathname.
     *
     * @param asyncApiDoc AsyncAPI document
     * @return pathname, or null if not found
     */
    @SuppressWarnings("unchecked")
    public String extractPathname(Map<String, Object> asyncApiDoc) {
        Map<String, Object> servers = yamlParser.getServers(asyncApiDoc);
        if (servers == null || servers.isEmpty()) {
            return null;
        }

        // Get first server
        Map<String, Object> firstServer = servers.values().stream()
                .filter(server -> server instanceof Map)
                .map(server -> (Map<String, Object>) server)
                .findFirst()
                .orElse(null);

        if (firstServer != null) {
            return (String) firstServer.get("pathname");
        }

        return null;
    }

    /**
     * Extracts protocol from server matching the given pathname.
     * <p>
     * Searches all servers for one with a matching pathname and returns its protocol.
     *
     * @param asyncApiDoc AsyncAPI document
     * @param pathname the pathname to search for
     * @return protocol (ws or wss), or null if no matching server found
     */
    @SuppressWarnings("unchecked")
    public String extractProtocolByPathname(Map<String, Object> asyncApiDoc, String pathname) {
        Map<String, Object> servers = yamlParser.getServers(asyncApiDoc);
        if (servers == null || servers.isEmpty()) {
            return null;
        }

        // Find server with matching pathname
        for (Map.Entry<String, Object> entry : servers.entrySet()) {
            if (entry.getValue() instanceof Map) {
                Map<String, Object> server = (Map<String, Object>) entry.getValue();
                String serverPathname = (String) server.get("pathname");
                if (pathname != null && pathname.equals(serverPathname)) {
                    return (String) server.get("protocol");
                }
            }
        }

        return null;
    }
}

