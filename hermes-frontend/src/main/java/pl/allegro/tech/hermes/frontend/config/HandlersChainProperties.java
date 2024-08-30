package pl.allegro.tech.hermes.frontend.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.frontend.publishing.handlers.HandlersChainParameters;

@ConfigurationProperties(prefix = "frontend.handlers")
public class HandlersChainProperties implements HandlersChainParameters {

  private Duration idleTimeout = Duration.ofMillis(65);

  private Duration longIdleTimeout = Duration.ofMillis(400);

  private Duration maxPublishRequestDuration = Duration.ofMillis(500);

  private boolean forceTopicMaxMessageSize = false;

  @Override
  public Duration getIdleTimeout() {
    return idleTimeout;
  }

  public void setIdleTimeout(Duration idleTimeout) {
    this.idleTimeout = idleTimeout;
  }

  @Override
  public Duration getLongIdleTimeout() {
    return longIdleTimeout;
  }

  public void setLongIdleTimeout(Duration longIdleTimeout) {
    this.longIdleTimeout = longIdleTimeout;
  }

  @Override
  public boolean isForceTopicMaxMessageSize() {
    return forceTopicMaxMessageSize;
  }

  @Override
  public boolean isKeepAliveHeaderEnabled() {
    return keepAliveHeader.enabled;
  }

  @Override
  public Duration getKeepAliveHeaderTimeout() {
    return keepAliveHeader.timeout;
  }

  @Override
  public boolean isAuthenticationEnabled() {
    return authentication.enabled;
  }

  @Override
  public String getAuthenticationMode() {
    return authentication.mode;
  }

  public void setForceTopicMaxMessageSize(boolean forceTopicMaxMessageSize) {
    this.forceTopicMaxMessageSize = forceTopicMaxMessageSize;
  }

  @Override
  public Duration getMaxPublishRequestDuration() {
    return maxPublishRequestDuration;
  }

  public void setMaxPublishRequestDuration(Duration maxPublishRequestDuration) {
    this.maxPublishRequestDuration = maxPublishRequestDuration;
  }

  private AuthenticationProperties authentication = new AuthenticationProperties();

  private KeepAliveHeaderProperties keepAliveHeader = new KeepAliveHeaderProperties();

  public AuthenticationProperties getAuthentication() {
    return authentication;
  }

  public void setAuthentication(AuthenticationProperties authentication) {
    this.authentication = authentication;
  }

  public KeepAliveHeaderProperties getKeepAliveHeader() {
    return keepAliveHeader;
  }

  public void setKeepAliveHeader(KeepAliveHeaderProperties keepAliveHeader) {
    this.keepAliveHeader = keepAliveHeader;
  }

  public static class AuthenticationProperties {

    private boolean enabled = false;

    private String mode = "constraint_driven";

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getMode() {
      return mode;
    }

    public void setMode(String mode) {
      this.mode = mode;
    }
  }

  public static class KeepAliveHeaderProperties {

    private boolean enabled = false;

    private Duration timeout = Duration.ofSeconds(1);

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public Duration getTimeout() {
      return timeout;
    }

    public void setTimeout(Duration timeout) {
      this.timeout = timeout;
    }
  }
}
