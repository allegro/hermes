package pl.allegro.tech.hermes.common.config;

public class KafkaSSLProperties {
    private boolean isEnabled = false;
    private String trustStoreLocation = null;
    private String trustStorePassword = null;
    private String keyStoreLocation = null;
    private String keyStorePassword = null;
    private String keyPassword = null;
    private String protocolVersion = "TLSv1.2";
    private String endpointIdentificationAlgorithm = "https";
    private String keyStoreCertificateChain = null;
    private String keyStoreKey = null;
    private String trustStoreCertificates = null;
    private String enabledProtocols = null;
    private String keyStoreType = "JKS";
    private String protocol = null;
    private String provider = null;
    private String trustStoreType = "JKS";
    private String cipherSuites = null;
    private String engineFactoryClass = null;
    private String keymanagerAlgorithm = "SunX509";
    private String secureRandomImplementation = null;
    private String trustmanagerAlgorithm = "PKIX";

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public String getTrustStoreLocation() {
        return trustStoreLocation;
    }

    public void setTrustStoreLocation(String trustStoreLocation) {
        this.trustStoreLocation = trustStoreLocation;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }

    public String getKeyStoreLocation() {
        return keyStoreLocation;
    }

    public void setKeyStoreLocation(String keyStoreLocation) {
        this.keyStoreLocation = keyStoreLocation;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public String getKeyPassword() {
        return keyPassword;
    }

    public void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public String getEndpointIdentificationAlgorithm() {
        return endpointIdentificationAlgorithm;
    }

    public void setEndpointIdentificationAlgorithm(String endpointIdentificationAlgorithm) {
        this.endpointIdentificationAlgorithm = endpointIdentificationAlgorithm;
    }

    public String getKeyStoreCertificateChain() {
        return keyStoreCertificateChain;
    }

    public void setKeyStoreCertificateChain(String keyStoreCertificateChain) {
        this.keyStoreCertificateChain = keyStoreCertificateChain;
    }

    public String getKeyStoreKey() {
        return keyStoreKey;
    }

    public void setKeyStoreKey(String keyStoreKey) {
        this.keyStoreKey = keyStoreKey;
    }

    public String getTrustStoreCertificates() {
        return trustStoreCertificates;
    }

    public void setTrustStoreCertificates(String trustStoreCertificates) {
        this.trustStoreCertificates = trustStoreCertificates;
    }

    public String getEnabledProtocols() {
        return enabledProtocols;
    }

    public void setEnabledProtocols(String enabledProtocols) {
        this.enabledProtocols = enabledProtocols;
    }

    public String getKeyStoreType() {
        return keyStoreType;
    }

    public void setKeyStoreType(String keyStoreType) {
        this.keyStoreType = keyStoreType;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getTrustStoreType() {
        return trustStoreType;
    }

    public void setTrustStoreType(String trustStoreType) {
        this.trustStoreType = trustStoreType;
    }

    public String getCipherSuites() {
        return cipherSuites;
    }

    public void setCipherSuites(String cipherSuites) {
        this.cipherSuites = cipherSuites;
    }

    public String getEngineFactoryClass() {
        return engineFactoryClass;
    }

    public void setEngineFactoryClass(String engineFactoryClass) {
        this.engineFactoryClass = engineFactoryClass;
    }

    public String getKeymanagerAlgorithm() {
        return keymanagerAlgorithm;
    }

    public void setKeymanagerAlgorithm(String keymanagerAlgorithm) {
        this.keymanagerAlgorithm = keymanagerAlgorithm;
    }

    public String getSecureRandomImplementation() {
        return secureRandomImplementation;
    }

    public void setSecureRandomImplementation(String secureRandomImplementation) {
        this.secureRandomImplementation = secureRandomImplementation;
    }

    public String getTrustmanagerAlgorithm() {
        return trustmanagerAlgorithm;
    }

    public void setTrustmanagerAlgorithm(String trustmanagerAlgorithm) {
        this.trustmanagerAlgorithm = trustmanagerAlgorithm;
    }

}
