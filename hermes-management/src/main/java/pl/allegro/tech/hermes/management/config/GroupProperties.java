package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "group")
public class GroupProperties {

  private boolean nonAdminCreationEnabled = false;

  private String allowedGroupNameRegex = "^[a-zA-Z0-9.]+$";

  public boolean isNonAdminCreationEnabled() {
    return nonAdminCreationEnabled;
  }

  public void setNonAdminCreationEnabled(boolean nonAdminCreationEnabled) {
    this.nonAdminCreationEnabled = nonAdminCreationEnabled;
  }

  public String getAllowedGroupNameRegex() {
    return allowedGroupNameRegex;
  }

  public void setAllowedGroupNameRegex(String allowedGroupNameRegex) {
    this.allowedGroupNameRegex = allowedGroupNameRegex;
  }
}
