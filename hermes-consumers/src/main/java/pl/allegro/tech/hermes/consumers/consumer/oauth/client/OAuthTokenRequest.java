package pl.allegro.tech.hermes.consumers.consumer.oauth.client;

public class OAuthTokenRequest {

  public static class Param {

    public static final String GRANT_TYPE = "grant_type";
    public static final String SCOPE = "scope";
    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
  }

  public static class GrantTypeValue {

    public static final String RESOURCE_OWNER_USERNAME_PASSWORD = "password";
    public static final String CLIENT_CREDENTIALS = "client_credentials";
  }

  private final String url;

  private final String grantType;

  private final String scope;

  private final String clientId;

  private final String clientSecret;

  private final String username;

  private final String password;

  private final Integer requestTimeout;

  private final Integer socketTimeout;

  private OAuthTokenRequest(
      String url,
      String grantType,
      String scope,
      String clientId,
      String clientSecret,
      String username,
      String password,
      Integer requestTimeout,
      Integer socketTimeout) {
    this.url = url;
    this.grantType = grantType;
    this.scope = scope;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.username = username;
    this.password = password;
    this.requestTimeout = requestTimeout;
    this.socketTimeout = socketTimeout;
  }

  public String getUrl() {
    return url;
  }

  public String getGrantType() {
    return grantType;
  }

  public String getScope() {
    return scope;
  }

  public String getClientId() {
    return clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public Integer getRequestTimeout() {
    return requestTimeout;
  }

  public Integer getSocketTimeout() {
    return socketTimeout;
  }

  public static OAuthTokenRequestBuilder oAuthTokenRequest() {
    return new OAuthTokenRequestBuilder();
  }

  public static class OAuthTokenRequestBuilder {

    private String url;

    private String grantType;

    private String scope;

    private String clientId;

    private String clientSecret;

    private String username;

    private String password;

    private Integer requestTimeout;

    private Integer socketTimeout;

    public OAuthTokenRequestBuilder withUrl(String url) {
      this.url = url;
      return this;
    }

    public OAuthTokenRequestBuilder withGrantType(String grantType) {
      this.grantType = grantType;
      return this;
    }

    public OAuthTokenRequestBuilder withScope(String scope) {
      this.scope = scope;
      return this;
    }

    public OAuthTokenRequestBuilder withClientId(String clientId) {
      this.clientId = clientId;
      return this;
    }

    public OAuthTokenRequestBuilder withClientSecret(String clientSecret) {
      this.clientSecret = clientSecret;
      return this;
    }

    public OAuthTokenRequestBuilder withUsername(String username) {
      this.username = username;
      return this;
    }

    public OAuthTokenRequestBuilder withPassword(String password) {
      this.password = password;
      return this;
    }

    public OAuthTokenRequestBuilder withRequestTimeout(Integer requestTimeout) {
      this.requestTimeout = requestTimeout;
      return this;
    }

    public OAuthTokenRequestBuilder withSocketTimeout(Integer socketTimeout) {
      this.socketTimeout = socketTimeout;
      return this;
    }

    public OAuthTokenRequest build() {
      return new OAuthTokenRequest(
          url,
          grantType,
          scope,
          clientId,
          clientSecret,
          username,
          password,
          requestTimeout,
          socketTimeout);
    }
  }
}
