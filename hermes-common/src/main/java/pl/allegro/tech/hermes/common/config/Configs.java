package pl.allegro.tech.hermes.common.config;

import pl.allegro.tech.hermes.common.util.InetAddressInstanceIdResolver;

import java.util.Arrays;

@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
public enum Configs {

    // removed already in master branch
    HOSTNAME("hostname", new InetAddressInstanceIdResolver().resolve()),

    KAFKA_CLUSTER_NAME("kafka.cluster.name", "primary-dc"),
    KAFKA_BROKER_LIST("kafka.broker.list", "localhost:9092"),
    KAFKA_NAMESPACE("kafka.namespace", ""),
    KAFKA_NAMESPACE_SEPARATOR("kafka.namespace.separator", "_"),

    KAFKA_ADMIN_REQUEST_TIMEOUT_MS("kafka.admin.request.timeout.ms", 5 * 60 * 1000),

    KAFKA_AUTHORIZATION_ENABLED("kafka.authorization.enabled", false),
    KAFKA_AUTHORIZATION_MECHANISM("kafka.authorization.mechanism", "PLAIN"),
    KAFKA_AUTHORIZATION_PROTOCOL("kafka.authorization.protocol", "SASL_PLAINTEXT"),
    KAFKA_AUTHORIZATION_USERNAME("kafka.authorization.username", "username"),
    KAFKA_AUTHORIZATION_PASSWORD("kafka.authorization.password", "password"),

    OAUTH_MISSING_SUBSCRIPTION_HANDLERS_CREATION_DELAY("oauth.missing.subscription.handlers.creation.delay", 10_000L),
    OAUTH_SUBSCRIPTION_TOKENS_CACHE_MAX_SIZE("oauth.subscription.tokens.cache.max.size", 1000L),
    OAUTH_PROVIDERS_TOKEN_REQUEST_RATE_LIMITER_RATE_REDUCTION_FACTOR(
            "oauth.providers.token.request.rate.limiter.rate.reduction.factor", 2.0),

    MESSAGE_CONTENT_ROOT("message.content.root", "message"),
    METADATA_CONTENT_ROOT("metadata.content.root", "metadata"),

    //consumer
    UNDELIVERED_MESSAGE_LOG_PERSIST_PERIOD_MS("undelivered.message.log.persist.period.ms", 5000);

    private final String name;

    private final Object defaultValue;

    Configs(String name, Object defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
    }

    public static Configs getForName(String name) {
        return Arrays.stream(Configs.values())
                .filter(configs -> configs.name.equals(name))
                .reduce((a, b) -> { throw new DuplicateConfigPropertyException(name); })
                .orElseThrow(() -> new MissingConfigPropertyException(name));
    }

    public String getName() {
        return name;
    }

    @SuppressWarnings("unchecked")
    public <T> T getDefaultValue() {
        return (T) defaultValue;
    }
}
