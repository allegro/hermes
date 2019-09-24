package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "console")
public class ConsoleProperties {

    private String configurationLocation = "console/config-local.json";
    private ConfigurationType configurationType = ConfigurationType.CLASSPATH_RESOURCE;
    private HttpClientProperties httpClient = new HttpClientProperties();

    public String getConfigurationLocation() {
        return configurationLocation;
    }

    public void setConfigurationLocation(String configurationLocation) {
        this.configurationLocation = configurationLocation;
    }

    public ConfigurationType getConfigurationType() {
        return configurationType;
    }

    public void setConfigurationType(ConfigurationType configurationType) {
        this.configurationType = configurationType;
    }

    public HttpClientProperties getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(HttpClientProperties httpClient) {
        this.httpClient = httpClient;
    }

    public static class HttpClientProperties {

        private Duration connectTimeout = Duration.ofMillis(500);
        private Duration readTimeout = Duration.ofSeconds(3);

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
        }
    }

    public enum ConfigurationType {
        CLASSPATH_RESOURCE, HTTP_RESOURCE
    }
}
