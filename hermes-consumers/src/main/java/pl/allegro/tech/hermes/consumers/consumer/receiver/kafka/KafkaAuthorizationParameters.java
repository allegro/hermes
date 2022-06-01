package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

public class KafkaAuthorizationParameters {

    private final boolean enabled;
    private final String mechanism;
    private final String protocol;
    private final String username;
    private final String password;

    public KafkaAuthorizationParameters(boolean enabled, String mechanism, String protocol, String username, String password) {
        this.enabled = enabled;
        this.mechanism = mechanism;
        this.protocol = protocol;
        this.username = username;
        this.password = password;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getMechanism() {
        return mechanism;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
