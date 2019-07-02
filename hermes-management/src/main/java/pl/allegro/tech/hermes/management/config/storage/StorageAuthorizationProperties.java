package pl.allegro.tech.hermes.management.config.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties
public class StorageAuthorizationProperties {

    private String scheme;
    private String user;
    private String password;

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
