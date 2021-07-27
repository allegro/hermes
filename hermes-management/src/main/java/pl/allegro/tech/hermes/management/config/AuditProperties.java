package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URL;

@ConfigurationProperties(prefix = "audit")
class AuditProperties {

    private URL eventUrl = null;

    public URL getEventUrl() {
        return eventUrl;
    }

    public void setEventUrl(URL eventUrl) {
        this.eventUrl = eventUrl;
    }
}
