package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;

public class SubscriptionOAuthPolicy {

  private static final String ANONYMIZED_PASSWORD = "******";
  @NotNull private final GrantType grantType;
  @NotNull private final String providerName;
  private final String scope;
  private final String username;
  private final String password;

  @JsonCreator
  public SubscriptionOAuthPolicy(
      @JsonProperty("grantType") GrantType grantType,
      @JsonProperty("providerName") String providerName,
      @JsonProperty("scope") String scope,
      @JsonProperty("username") String username,
      @JsonProperty("password") String password) {

    this.grantType = grantType;
    this.providerName = providerName;
    this.scope = scope;
    this.username = username;
    this.password = password;
  }

  public static Builder passwordGrantOAuthPolicy(String providerName) {
    return new Builder(providerName, GrantType.USERNAME_PASSWORD);
  }

  public static Builder clientCredentialsGrantOAuthPolicy(String providerName) {
    return new Builder(providerName, GrantType.CLIENT_CREDENTIALS);
  }

  public GrantType getGrantType() {
    return grantType;
  }

  public String getProviderName() {
    return providerName;
  }

  public String getScope() {
    return scope;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public SubscriptionOAuthPolicy anonymize() {
    if (GrantType.USERNAME_PASSWORD.equals(grantType)) {
      return new SubscriptionOAuthPolicy(
          grantType, providerName, scope, username, ANONYMIZED_PASSWORD);
    }
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SubscriptionOAuthPolicy that = (SubscriptionOAuthPolicy) o;
    return grantType == that.grantType
        && Objects.equals(providerName, that.providerName)
        && Objects.equals(scope, that.scope)
        && Objects.equals(username, that.username)
        && Objects.equals(password, that.password);
  }

  @Override
  public int hashCode() {
    return Objects.hash(grantType, providerName, scope, username, password);
  }

  public enum GrantType {
    CLIENT_CREDENTIALS("clientCredentials"),
    USERNAME_PASSWORD("password");

    private final String name;

    GrantType(String name) {
      this.name = name;
    }

    @JsonValue
    public String getName() {
      return name;
    }
  }

  public static class Builder {

    private final String providerName;
    private final GrantType grantType;
    private String scope;
    private String username;
    private String password;

    public Builder(String providerName, GrantType grantType) {
      this.providerName = providerName;
      this.grantType = grantType;
    }

    public Builder withScope(String scope) {
      this.scope = scope;
      return this;
    }

    public Builder withUsername(String username) {
      this.username = username;
      return this;
    }

    public Builder withPassword(String password) {
      this.password = password;
      return this;
    }

    public SubscriptionOAuthPolicy build() {
      return new SubscriptionOAuthPolicy(grantType, providerName, scope, username, password);
    }
  }
}
