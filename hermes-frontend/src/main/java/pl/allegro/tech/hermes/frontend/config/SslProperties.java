package pl.allegro.tech.hermes.frontend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.frontend.server.SslParameters;

@ConfigurationProperties(prefix = "frontend.ssl")
public class SslProperties {

    private boolean enabled = false;

    private int port = 8443;

    private String clientAuthMode = "not_requested";

    private String protocol = "TLS";

    private String keystoreSource = "jre";

    private String keystoreLocation = "classpath:server.keystore";

    private String keystorePassword = "password";

    private String keystoreFormat = "JKS";

    private String truststoreSource = "jre";

    private String truststoreLocation = "classpath:server.truststore";

    private String truststorePassword = "password";

    private String truststoreFormat = "JKS";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getClientAuthMode() {
        return clientAuthMode;
    }

    public void setClientAuthMode(String clientAuthMode) {
        this.clientAuthMode = clientAuthMode;
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

    public SslParameters toSslParameters() {
        return new SslParameters(
                this.enabled,
                this.port,
                this.clientAuthMode,
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

