package pl.allegro.tech.hermes.management.config.console;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "console.config")
public class ConsoleConfigProperties {

  private String location = "console/config-local.json";
  private ConfigurationType type = ConfigurationType.SPRING_CONFIG;
  private HttpClientProperties httpClient = new HttpClientProperties();

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public ConfigurationType getType() {
    return type;
  }

  public void setType(ConfigurationType type) {
    this.type = type;
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
    CLASSPATH_RESOURCE,
    HTTP_RESOURCE,
    SPRING_CONFIG
  }
}
