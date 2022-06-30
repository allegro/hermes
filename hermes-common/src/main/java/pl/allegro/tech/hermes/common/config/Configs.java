package pl.allegro.tech.hermes.common.config;

import java.util.Arrays;

@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
public enum Configs {

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
