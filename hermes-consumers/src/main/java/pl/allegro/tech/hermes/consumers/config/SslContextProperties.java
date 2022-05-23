package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.SslContextParameters;

@ConfigurationProperties(prefix = "consumer.ssl")
public class SslContextProperties {

    private boolean enabled = true;

    private String protocol = "TLS";

    private String keystoreSource = "jre";

    private String keystoreLocation = "classpath:client.keystore";

    private String keystorePassword = "password";

    private String keystoreFormat = "JKS";

    private String truststoreSource = "jre";

    private String truststoreLocation = "classpath:client.truststore";

    private String truststorePassword = "password";

    private String truststoreFormat = "JKS";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getKeystoreSource() {
        return keystoreSource;
    }

    public void setKeystoreSource(String keystoreSource) {
        this.keystoreSource = keystoreSource;
    }

    public String getKeystoreLocation() {
        return keystoreLocation;
    }

    public void setKeystoreLocation(String keystoreLocation) {
        this.keystoreLocation = keystoreLocation;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    public String getKeystoreFormat() {
        return keystoreFormat;
    }

    public void setKeystoreFormat(String keystoreFormat) {
        this.keystoreFormat = keystoreFormat;
    }

    public String getTruststoreSource() {
        return truststoreSource;
    }

    public void setTruststoreSource(String truststoreSource) {
        this.truststoreSource = truststoreSource;
    }

    public String getTruststoreLocation() {
        return truststoreLocation;
    }

    public void setTruststoreLocation(String truststoreLocation) {
        this.truststoreLocation = truststoreLocation;
    }

    public String getTruststorePassword() {
        return truststorePassword;
    }

    public void setTruststorePassword(String truststorePassword) {
        this.truststorePassword = truststorePassword;
    }

    public String getTruststoreFormat() {
        return truststoreFormat;
    }

    public void setTruststoreFormat(String truststoreFormat) {
        this.truststoreFormat = truststoreFormat;
    }

    public SslContextParameters toSslContextParams() {
        return new SslContextParameters(
                this.enabled,
                this.protocol,
                this.keystoreSource,
                this.keystoreLocation,
                this.keystorePassword,
                this.keystoreFormat,
                this.truststoreSource,
                this.truststoreLocation,
                this.truststorePassword,
                this.truststoreFormat
        );
    }
}
