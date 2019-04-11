package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Optional;

public class OAuthProviderWithOptionalSocketTimeout {

    private final static int DEFAULT_SOCKET_TIMEOUT = 0;

    private final String name;
    private final String tokenEndpoint;
    private final String clientId;
    private final String clientSecret;
    private final Integer tokenRequestInitialDelay;
    private final Integer tokenRequestMaxDelay;
    private Integer requestTimeout;
    private Integer socketTimeout;

    @JsonCreator
    public OAuthProviderWithOptionalSocketTimeout(@JsonProperty("name") String name,
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
        this.socketTimeout = Optional.ofNullable(socketTimeout).orElse(DEFAULT_SOCKET_TIMEOUT);
    }

    public OAuthProvider toBasicOAuthProvider() {
        return new OAuthProvider(name, tokenEndpoint, clientId, clientSecret, tokenRequestInitialDelay,
                tokenRequestMaxDelay, requestTimeout, socketTimeout);
    }
}
