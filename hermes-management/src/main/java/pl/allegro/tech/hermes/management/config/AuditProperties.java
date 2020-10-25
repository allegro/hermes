package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URL;

@ConfigurationProperties(prefix = "audit")
class AuditProperties {

    private boolean enabled = false;

    private URL eventUrl = null;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public URL getEventUrl() {
        return eventUrl;
    }

    public void setEventUrl(URL eventUrl) {
        this.eventUrl = eventUrl;
    }
}
