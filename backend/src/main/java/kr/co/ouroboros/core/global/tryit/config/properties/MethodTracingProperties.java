package kr.co.ouroboros.core.global.tryit.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for method-level tracing.
 * <p>
 * This class holds configuration properties for automatic method-level tracing
 * using AOP and Micrometer Observation.
 * <p>
 * <b>Configuration Properties:</b>
 * <ul>
 *   <li>{@code ouroboros.method-tracing.enabled} - Enable/disable method tracing (default: false)</li>
 *   <li>{@code ouroboros.method-tracing.mode} - Tracing mode: SPRING_AOP or ASPECTJ (default: SPRING_AOP)</li>
 *   <li>{@code ouroboros.method-tracing.allowed-packages} - List of package prefixes to trace (default: empty)</li>
 * </ul>
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 * ouroboros.method-tracing.enabled=true
 * ouroboros.method-tracing.mode=SPRING_AOP
 * ouroboros.method-tracing.allowed-packages[0]=com.example.service
 * ouroboros.method-tracing.allowed-packages[1]=com.example.repository
 * }</pre>
 * <p>
 * Method tracing is disabled by default. To enable it, set {@code ouroboros.method-tracing.enabled=true}
 * and configure at least one allowed package. Only classes in allowed packages will be traced.
 * If allowed-packages is empty, method tracing is effectively disabled.
 * <p>
 * <b>Tracing Modes:</b>
 * <ul>
 *   <li>{@link Mode#SPRING_AOP} - Uses Spring AOP with CGLIB/JDK dynamic proxies (default)</li>
 *   <li>{@link Mode#ASPECTJ} - Uses AspectJ compile-time weaving for comprehensive tracing</li>
 * </ul>
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
@ConfigurationProperties(prefix = "ouroboros.method-tracing")
public class MethodTracingProperties {

    /**
     * Tracing mode enumeration.
     * <p>
     * Defines the method tracing implementation strategy.
     *
     * @since 1.1.0
     */
    public enum Mode {
        /**
         * Spring AOP mode using CGLIB/JDK dynamic proxies.
         * <p>
         * <b>Capabilities:</b>
         * <ul>
         *   <li>Traces public methods called through Spring proxies</li>
         *   <li>No additional build configuration required</li>
         *   <li>Works out-of-the-box</li>
         * </ul>
         * <p>
         * <b>Limitations:</b>
         * <ul>
         *   <li>Cannot trace self-invocation (same class method calls)</li>
         *   <li>Cannot trace private methods</li>
         *   <li>Cannot trace static methods</li>
         *   <li>Proxy creation overhead at runtime</li>
         * </ul>
         */
        SPRING_AOP,

        /**
         * AspectJ compile-time weaving mode.
         * <p>
         * <b>Capabilities:</b>
         * <ul>
         *   <li>Traces self-invocation (same class method calls)</li>
         *   <li>Traces private methods</li>
         *   <li>Traces static methods</li>
         *   <li>No proxy overhead at runtime</li>
         *   <li>Direct bytecode modification</li>
         * </ul>
         * <p>
         * <b>Requirements:</b>
         * <ul>
         *   <li>AspectJ weaving plugin configured in build.gradle or pom.xml</li>
         *   <li>Increased build time due to compile-time weaving</li>
         *   <li>See documentation for detailed setup instructions</li>
         * </ul>
         */
        ASPECTJ
    }

    /**
     * Whether method tracing is enabled.
     * <p>
     * Default: false
     */
    private boolean enabled = false;

    /**
     * Tracing mode (SPRING_AOP or ASPECTJ).
     * <p>
     * Default: SPRING_AOP
     *
     * @since 1.1.0
     */
    private Mode mode = Mode.SPRING_AOP;

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
     * Gets the current tracing mode.
     *
     * @return the tracing mode (SPRING_AOP or ASPECTJ)
     * @since 1.1.0
     */
    public Mode getMode() {
        return mode;
    }

    /**
     * Sets the tracing mode.
     *
     * @param mode the tracing mode to use (SPRING_AOP or ASPECTJ)
     * @since 1.1.0
     */
    public void setMode(Mode mode) {
        this.mode = (mode != null) ? mode : Mode.SPRING_AOP;
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
