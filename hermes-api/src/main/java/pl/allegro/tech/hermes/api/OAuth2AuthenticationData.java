package pl.allegro.tech.hermes.api;

import java.util.Map;
import java.util.Objects;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonCreator;

public class OAuth2AuthenticationData implements AuthenticationData {

    private static final String ANONYMIZED_PATTERN = "*****";

    private final String username;

    private final String password;

    private final String consumerKey;

    private final String consumerSecret;

    private final String grantType;

    @Valid
    private final EndpointAddress accessTokenEndpoint;

    public OAuth2AuthenticationData(String username,
            String password,
            String consumerKey,
            String consumerSecret,
            String grantType,
            EndpointAddress accessTokenEndpoint) {
        this.username = username;
        this.password = password;
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
        this.grantType = grantType;
        this.accessTokenEndpoint = accessTokenEndpoint;
    }

    public OAuth2AuthenticationData(String username,
            String consumerKey,
            String grantType,
            EndpointAddress accessTokenEndpoint) {
        this.username = username;
        this.password = ANONYMIZED_PATTERN;
        this.consumerKey = consumerKey;
        this.consumerSecret = ANONYMIZED_PATTERN;
        this.grantType = grantType;
        this.accessTokenEndpoint = accessTokenEndpoint;
    }

    @JsonCreator
    public static OAuth2AuthenticationData create(Map<String, String> properties) {
        return new OAuth2AuthenticationData(
                (String) properties.getOrDefault("username", ""),
                (String) properties.getOrDefault("password", ""),
                (String) properties.getOrDefault("consumerKey", ""),
                (String) properties.getOrDefault("consumerSecret", ""),
                (String) properties.getOrDefault("grantType", ""),
                new EndpointAddress(properties.getOrDefault("accessTokenEndpoint", ""))
        );
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getConsumerKey() {
        return consumerKey;
    }

    public String getConsumerSecret() {
        return consumerSecret;
    }

    public String getGrantType() {
        return grantType;
    }

    public String getAccessTokenEndpoint() {
        return accessTokenEndpoint.getEndpoint();
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, password, consumerKey, consumerSecret, grantType, accessTokenEndpoint);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final OAuth2AuthenticationData other = (OAuth2AuthenticationData) obj;
        return Objects.equals(this.username, other.username)
                && Objects.equals(this.password, other.password)
                && Objects.equals(this.consumerKey, other.consumerKey)
                && Objects.equals(this.consumerSecret, other.consumerSecret)
                && Objects.equals(this.grantType, other.grantType)
                && Objects.equals(this.accessTokenEndpoint, other.accessTokenEndpoint);
    }

    public OAuth2AuthenticationData anonymize() {
        return new OAuth2AuthenticationData(username, consumerKey, grantType, accessTokenEndpoint);
    }
}
