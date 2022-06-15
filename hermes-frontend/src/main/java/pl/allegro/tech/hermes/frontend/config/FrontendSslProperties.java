package pl.allegro.tech.hermes.frontend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "frontend.ssl")
public class FrontendSslProperties {
    private boolean enabled = false;
    private String prot = "8443";
    private String clientAuthMode = "not_requested";
    private String protocol = "TLS";
    private SslProperties keystore;
    private SslProperties truststore;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProt() {
        return prot;
    }

    public void setProt(String prot) {
        this.prot = prot;
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

    public SslProperties getKeystore() {
        return keystore;
    }

    public void setKeystore(SslProperties keystore) {
        this.keystore = keystore;
    }

    public SslProperties getTruststore() {
        return truststore;
    }

    public void setTruststore(SslProperties truststore) {
        this.truststore = truststore;
    }
}
