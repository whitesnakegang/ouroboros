package kr.co.ouroboros.core.rest.tryit.infrastructure.instrumentation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ouroboros.method-tracing")
public class MethodTracingProperties {

    private boolean enabled = true;
    private java.util.List<String> allowedPackages = new java.util.ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public java.util.List<String> getAllowedPackages() {
        return allowedPackages;
    }

    public void setAllowedPackages(java.util.List<String> allowedPackages) {
        this.allowedPackages = (allowedPackages != null) ? allowedPackages : new java.util.ArrayList<>();
    }
}

