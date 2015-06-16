package pl.allegro.tech.hermes.frontend.server;


public class KeystoreProperties {
    private final String location;
    private final String format;
    private final String password;

    public KeystoreProperties(String location, String format, String password) {
        this.location = location;
        this.format = format;
        this.password = password;
    }

    public String getLocation() {
        return location;
    }

    public String getFormat() {
        return format;
    }

    public String getPassword() {
        return password;
    }
}
