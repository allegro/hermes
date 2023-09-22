package pl.allegro.tech.hermes.common.config;

public class KafkaAuthorizationProperties {

    private boolean enabled = false;
    private String mechanism = "PLAIN";
    private String protocol = "SASL_PLAINTEXT";
    private String username = "username";
    private String password = "password";
    private String loginModule = "org.apache.kafka.common.security.plain.PlainLoginModule";
    private String jaasConfig;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMechanism() {
        return mechanism;
    }

    public void setMechanism(String mechanism) {
        this.mechanism = mechanism;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLoginModule() {
        return loginModule;
    }

    public void setLoginModule(String loginModule) {
        this.loginModule = loginModule;
    }

    public String getJaasConfig() {
        return jaasConfig;
    }

    public void setJaasConfig(String jaasConfig) {
        this.jaasConfig = jaasConfig;
    }
}
