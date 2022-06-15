package pl.allegro.tech.hermes.frontend.server;

public class ContextFactoryParameters {
    private final String frontendSslProtocol;
    private final String sslKeystoreSource;
    private final String sslKeystoreLocation;
    private final String sslKeystoreFormat;
    private final String sslKeystorePassword;
    private final String sslTrustStoreSource;
    private final String sslTrustStoreLocation;
    private final String sslTrustStoreFormat;
    private final String sslTrustStorePassword;


    public ContextFactoryParameters(String frontendSslProtocol,
                                    String sslKeystoreSource,
                                    String sslKeystoreLocation,
                                    String sslKeystoreFormat,
                                    String sslKeystorePassword,
                                    String sslTrustStoreSource,
                                    String sslTrustStoreLocation,
                                    String sslTrustStoreFormat,
                                    String sslTrustStorePassword) {
        this.frontendSslProtocol = frontendSslProtocol;
        this.sslKeystoreSource = sslKeystoreSource;
        this.sslKeystoreLocation = sslKeystoreLocation;
        this.sslKeystoreFormat = sslKeystoreFormat;
        this.sslKeystorePassword = sslKeystorePassword;
        this.sslTrustStoreSource = sslTrustStoreSource;
        this.sslTrustStoreLocation = sslTrustStoreLocation;
        this.sslTrustStoreFormat = sslTrustStoreFormat;
        this.sslTrustStorePassword = sslTrustStorePassword;
    }

    public String getFrontendSslProtocol() {
        return frontendSslProtocol;
    }

    public String getSslKeystoreSource() {
        return sslKeystoreSource;
    }

    public String getSslKeystoreLocation() {
        return sslKeystoreLocation;
    }

    public String getSslKeystoreFormat() {
        return sslKeystoreFormat;
    }

    public String getSslKeystorePassword() {
        return sslKeystorePassword;
    }

    public String getSslTrustStoreSource() {
        return sslTrustStoreSource;
    }

    public String getSslTrustStoreLocation() {
        return sslTrustStoreLocation;
    }

    public String getSslTrustStoreFormat() {
        return sslTrustStoreFormat;
    }

    public String getSslTrustStorePassword() {
        return sslTrustStorePassword;
    }
}
