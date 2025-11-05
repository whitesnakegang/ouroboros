package kr.co.ouroboros.core.global.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Ouroboros library.
 * <p>
 * Allows users to customize library behavior via application.properties or application.yml.
 * <p>
 * Example configuration:
 * <pre>
 * ouroboros.enabled=true
 * ouroboros.server.url=http://localhost:8080
 * ouroboros.server.description=Development Server
 * </pre>
 *
 * @since 0.0.1
 */
@ConfigurationProperties(prefix = "ouroboros")
public class OuroborosProperties {

    /**
     * Enable or disable Ouroboros library.
     * When disabled, all beans and endpoints will not be registered.
     */
    private boolean enabled = true;

    private Server server = new Server();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    /**
     * Server-related configuration properties.
     */
    public static class Server {
        /**
         * Base URL of the server for OpenAPI specification.
         * If not specified, defaults to http://127.0.0.1:{server.port}
         */
        private String url;

        /**
         * Description of the server in OpenAPI specification.
         * Defaults to "Local Mock" if not specified.
         */
        private String description = "Local Mock";

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
