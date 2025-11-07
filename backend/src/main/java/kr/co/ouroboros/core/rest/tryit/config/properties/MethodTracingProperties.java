package kr.co.ouroboros.core.rest.tryit.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for method-level tracing.
 * <p>
 * This class holds configuration properties for automatic method-level tracing
 * using AOP and Micrometer Observation.
 * <p>
 * <b>Configuration Properties:</b>
 * <ul>
 *   <li>{@code ouroboros.method-tracing.enabled} - Enable/disable method tracing (default: true)</li>
 *   <li>{@code ouroboros.method-tracing.allowed-packages} - List of package prefixes to trace (default: empty)</li>
 * </ul>
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 * ouroboros.method-tracing.enabled=true
 * ouroboros.method-tracing.allowed-packages[0]=com.example.service
 * ouroboros.method-tracing.allowed-packages[1]=com.example.repository
 * }</pre>
 * <p>
 * Only classes in allowed packages will be traced. If allowed-packages is empty,
 * method tracing is effectively disabled.
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
@ConfigurationProperties(prefix = "ouroboros.method-tracing")
public class MethodTracingProperties {

    /**
     * Whether method tracing is enabled.
     * <p>
     * Default: true
     */
    private boolean enabled = true;
    
    /**
     * List of package prefixes for classes to trace.
     * <p>
     * Only classes (and their interfaces/superclasses) that belong to
     * packages starting with these prefixes will be traced.
     * <p>
     * Example: ["com.example.service", "com.example.repository"]
     * <p>
     * Default: empty list (method tracing disabled)
     */
    private java.util.List<String> allowedPackages = new java.util.ArrayList<>();

    /**
     * Indicates whether method-level tracing is enabled.
     *
     * @return `true` if method tracing is enabled, `false` otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether method tracing is enabled.
     *
     * @param enabled true to enable, false to disable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
         * Package prefixes that determine which classes are eligible for method-level tracing.
         * <p>
         * Only classes whose package starts with one of these prefixes, and their related interfaces or subclasses, will be traced.
         *
         * @return the list of package prefixes; never {@code null}, may be empty
         */
    public java.util.List<String> getAllowedPackages() {
        return allowedPackages;
    }

    /**
     * Set the package prefixes for which method tracing is enabled.
     *
     * If `allowedPackages` is `null` it is replaced with an empty list, which disables tracing.
     *
     * @param allowedPackages package prefixes to enable tracing for, or `null` to clear the list
     */
    public void setAllowedPackages(java.util.List<String> allowedPackages) {
        this.allowedPackages = (allowedPackages != null) ? allowedPackages : new java.util.ArrayList<>();
    }
}

