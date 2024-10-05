package pl.allegro.tech.hermes.api;

import static pl.allegro.tech.hermes.api.constraints.Names.ALLOWED_NAME_REGEX;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.Objects;

public class OAuthProvider implements Anonymizable {

  private static final String ANONYMIZED_CLIENT_SECRET = "******";
  private static final int DEFAULT_SOCKET_TIMEOUT = 0;

  @NotEmpty
  @Pattern(regexp = ALLOWED_NAME_REGEX)
  private final String name;

  @NotEmpty private final String tokenEndpoint;

  @NotEmpty private final String clientId;

  @NotEmpty private final String clientSecret;

  @Min(0)
  @Max(3600_000)
  @NotNull
  private final Integer tokenRequestInitialDelay;

  @Min(0)
  @Max(3600_000)
  @NotNull
  private final Integer tokenRequestMaxDelay;

  @Min(100)
  @Max(60_000)
  @NotNull
  private Integer requestTimeout;

  @Min(0)
  @Max(60_000)
  @NotNull
  private Integer socketTimeout;

  @JsonCreator
  public OAuthProvider(
      @JsonProperty("name") String name,
      @JsonProperty("tokenEndpoint") String tokenEndpoint,
      @JsonProperty("clientId") String clientId,
      @JsonProperty("clientSecret") String clientSecret,
      @JsonProperty("tokenRequestInitialDelay") Integer tokenRequestInitialDelay,
      @JsonProperty("tokenRequestMaxDelay") Integer tokenRequestMaxDelay,
      @JsonProperty("requestTimeout") Integer requestTimeout,
      @JsonProperty("socketTimeout") Integer socketTimeout) {
    this.name = name;
    this.tokenEndpoint = tokenEndpoint;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.tokenRequestInitialDelay = tokenRequestInitialDelay;
    this.tokenRequestMaxDelay = tokenRequestMaxDelay;
    this.requestTimeout = requestTimeout;
    this.socketTimeout = socketTimeout != null ? socketTimeout : DEFAULT_SOCKET_TIMEOUT;
  }

  public String getName() {
    return name;
  }

  public String getTokenEndpoint() {
    return tokenEndpoint;
  }

  public String getClientId() {
    return clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public Integer getTokenRequestInitialDelay() {
    return tokenRequestInitialDelay;
  }

  public Integer getTokenRequestMaxDelay() {
    return tokenRequestMaxDelay;
  }

  public Integer getRequestTimeout() {
    return requestTimeout;
  }

  public Integer getSocketTimeout() {
    return socketTimeout;
  }

  public OAuthProvider anonymize() {
    return new OAuthProvider(
        name,
        tokenEndpoint,
        clientId,
        ANONYMIZED_CLIENT_SECRET,
        tokenRequestInitialDelay,
        tokenRequestMaxDelay,
        requestTimeout,
        socketTimeout);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OAuthProvider that = (OAuthProvider) o;
    return Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }
}
