package pl.allegro.tech.hermes.consumers.consumer.sender.http;

public class SslContextParameters {
    private final boolean enabled;
    private final String protocol;
    private final String keystoreSource;
    private final String keystoreLocation;
    private final String keystorePassword;
    private final String keystoreFormat;
    private final String truststoreSource;
    private final String truststoreLocation;
    private final String truststorePassword;
    private final String truststoreFormat ;

    public SslContextParameters(boolean enabled, String protocol, String keystoreSource, String keystoreLocation, String keystorePassword, String keystoreFormat, String truststoreSource, String truststoreLocation, String truststorePassword, String truststoreFormat) {
        this.enabled = enabled;
        this.protocol = protocol;
        this.keystoreSource = keystoreSource;
        this.keystoreLocation = keystoreLocation;
        this.keystorePassword = keystorePassword;
        this.keystoreFormat = keystoreFormat;
        this.truststoreSource = truststoreSource;
        this.truststoreLocation = truststoreLocation;
        this.truststorePassword = truststorePassword;
        this.truststoreFormat = truststoreFormat;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getKeystoreSource() {
        return keystoreSource;
    }

    public String getKeystoreLocation() {
        return keystoreLocation;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public String getKeystoreFormat() {
        return keystoreFormat;
    }

    public String getTruststoreSource() {
        return truststoreSource;
    }

    public String getTruststoreLocation() {
        return truststoreLocation;
    }

    public String getTruststorePassword() {
        return truststorePassword;
    }

    public String getTruststoreFormat() {
        return truststoreFormat;
    }
}
