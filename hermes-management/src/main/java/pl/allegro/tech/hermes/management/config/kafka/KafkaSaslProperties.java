package pl.allegro.tech.hermes.management.config.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sasl")
public class KafkaSaslProperties {
    private boolean isEnabled = false;
    private String mechanism = "PLAIN";
    private String protocol = "SASL_PLAINTEXT";
    private String username = "admin";
    private String password = "admin-secret";
    private String jaasConfig;

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
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

    public String getJaasConfig() {
        return "org.apache.kafka.common.security.plain.PlainLoginModule required\n"
                + "username=\"" + username + "\"\n"
                + "password=\"" + password + "\";";
    }

    public void setJaasConfig(String jaasConfig) {
        this.jaasConfig = jaasConfig;
    }
}
