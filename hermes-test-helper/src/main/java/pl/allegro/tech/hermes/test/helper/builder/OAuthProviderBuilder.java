package pl.allegro.tech.hermes.test.helper.builder;

import pl.allegro.tech.hermes.api.OAuthProvider;

public class OAuthProviderBuilder {

  private final String name;

  private String tokenEndpoint = "http://example.com/token";

  private String clientId = "testClient123";

  private String clientSecret = "testPassword123";

  private int tokenRequestInitialDelay = 1;

  private int tokenRequestMaxDelay = 8;

  private int requestTimeout = 500;

  private int socketTimeout = 0;

  public OAuthProviderBuilder(String name) {
    this.name = name;
  }

  public static OAuthProviderBuilder oAuthProvider(String name) {
    return new OAuthProviderBuilder(name);
  }

  public OAuthProvider build() {
    return new OAuthProvider(
        name,
        tokenEndpoint,
        clientId,
        clientSecret,
        tokenRequestInitialDelay,
        tokenRequestMaxDelay,
        requestTimeout,
        socketTimeout);
  }

  public OAuthProviderBuilder withTokenEndpoint(String tokenEndpoint) {
    this.tokenEndpoint = tokenEndpoint;
    return this;
  }

  public OAuthProviderBuilder withClientId(String clientId) {
    this.clientId = clientId;
    return this;
  }

  public OAuthProviderBuilder withClientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
    return this;
  }

  public OAuthProviderBuilder withTokenRequestInitialDelay(int tokenRequestInitialDelay) {
    this.tokenRequestInitialDelay = tokenRequestInitialDelay;
    return this;
  }

  public OAuthProviderBuilder withTokenRequestMaxDelay(int tokenRequestMaxDelay) {
    this.tokenRequestMaxDelay = tokenRequestMaxDelay;
    return this;
  }

  public OAuthProviderBuilder withRequestTimeout(int requestTimeout) {
    this.requestTimeout = requestTimeout;
    return this;
  }

  public OAuthProviderBuilder withSocketTimeout(int socketTimeout) {
    this.socketTimeout = socketTimeout;
    return this;
  }
}
