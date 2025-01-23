package pl.allegro.tech.hermes.management.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jersey")
public class JerseyProperties {

  private List<String> packagesToScan = new ArrayList<>();
  private String filterStaticContentRegexp = "(/status/|/assets/|/favicon.ico).*";

  public List<String> getPackagesToScan() {
    return packagesToScan;
  }

  public void setPackagesToScan(List<String> packagesToScan) {
    this.packagesToScan = packagesToScan;
  }

  public String getFilterStaticContentRegexp() {
    return filterStaticContentRegexp;
  }

  public void setFilterStaticContentRegexp(String filterStaticContentRegexp) {
    this.filterStaticContentRegexp = filterStaticContentRegexp;
  }
}
