package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "management.leadership")
public class ManagementLeadershipProperties {

  private String zookeeperDc;

  public String getZookeeperDc() {
    return zookeeperDc;
  }

  public void setZookeeperDc(String zookeeperDc) {
    this.zookeeperDc = zookeeperDc;
  }
}
