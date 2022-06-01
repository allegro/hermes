package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.KafkaAuthorizationParameters;

@ConfigurationProperties(prefix = "kafka.authorization")
public class KafkaAuthorizationProperties {

    private boolean enabled = false;
    private String mechanism = "PLAIN";
    private String protocol = "SASL_PLAINTEXT";
    private String username = "username";
    private String password = "password";

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

    protected KafkaAuthorizationParameters toKafkaAuthorizationParameters() {
        return new KafkaAuthorizationParameters(
                this.enabled,
                this.mechanism,
                this.protocol,
                this.username,
                this.password
        );
    }
}
