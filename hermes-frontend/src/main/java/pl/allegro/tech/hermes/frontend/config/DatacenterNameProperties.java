package pl.allegro.tech.hermes.frontend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.infrastructure.dc.DcNameSource;

@ConfigurationProperties(prefix = "frontend.datacenter.name")
public class DatacenterNameProperties {

  private DcNameSource source;

  private String env = "DC";

  public DcNameSource getSource() {
    return source;
  }

  public void setSource(DcNameSource source) {
    this.source = source;
  }

  public String getEnv() {
    return env;
  }

  public void setEnv(String env) {
    this.env = env;
  }
}
