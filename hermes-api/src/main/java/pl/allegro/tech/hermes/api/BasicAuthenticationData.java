package pl.allegro.tech.hermes.api;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;

public class BasicAuthenticationData implements AuthenticationData{

    private static final String ANONYMIZED_PATTERN = "*****";

    private final String username;

    private final String password;

    public BasicAuthenticationData(String username,
            String password) {
        this.username = username;
        this.password = password;
    }

    public BasicAuthenticationData(String username) {
        this.username = username;
        this.password = ANONYMIZED_PATTERN;
    }

    @JsonCreator
    public static BasicAuthenticationData create(Map<String, String> properties) {
        return new BasicAuthenticationData(
                (String) properties.getOrDefault("username", ""),
                (String) properties.getOrDefault("password", "")
        );
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, password);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final BasicAuthenticationData other = (BasicAuthenticationData) obj;
        return Objects.equals(this.username, other.username)
                && Objects.equals(this.password, other.password);
    }

    public BasicAuthenticationData anonymize() {
        return new BasicAuthenticationData(username);
    }

}
