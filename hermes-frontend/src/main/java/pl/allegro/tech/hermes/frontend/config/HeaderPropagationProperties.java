package pl.allegro.tech.hermes.frontend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "frontend.header.propagation")
public class HeaderPropagationProperties {

    private boolean enabled = false;

    private String allowFilter = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAllowFilter() {
        return allowFilter;
    }

    public void setAllowFilter(String allowFilter) {
        this.allowFilter = allowFilter;
    }
}
