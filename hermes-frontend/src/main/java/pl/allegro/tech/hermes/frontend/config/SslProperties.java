package pl.allegro.tech.hermes.frontend.config;

public class SslProperties {

    private String source = "jre";
    private String location = "classpath:server.keystore";
    private String password = "password";
    private String format = "JKS";

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
