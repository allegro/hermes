package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "server")
public class ServerPortProperties {

  private String port = "8080";

  public String getPort() {
    return port;
  }

  public void setPort(String port) {
    this.port = port;
  }
}
