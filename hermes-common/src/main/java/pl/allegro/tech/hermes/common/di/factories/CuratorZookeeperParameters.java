package pl.allegro.tech.hermes.common.di.factories;

public class CuratorZookeeperParameters {
    private final boolean enabled;
    private final String scheme;
    private final String user;
    private final String password;
    private final String connectString;

    public CuratorZookeeperParameters(boolean enabled, String scheme, String user, String password, String connectString) {
        this.enabled = enabled;
        this.scheme = scheme;
        this.user = user;
        this.password = password;
        this.connectString = connectString;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getScheme() {
        return scheme;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getConnectString() {
        return connectString;
    }
}
