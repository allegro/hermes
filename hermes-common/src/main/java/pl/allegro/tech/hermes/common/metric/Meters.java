package pl.allegro.tech.hermes.common.metric;

import static pl.allegro.tech.hermes.metrics.PathsCompiler.GROUP;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.HTTP_CODE;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.HTTP_CODE_FAMILY;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.OAUTH_PROVIDER_NAME;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.SUBSCRIPTION;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.TOPIC;

public class Meters {

    public static final String METER = "meter";
    public static final String TOPIC_METER = METER + "." + GROUP + "." + TOPIC;
    public static final String SUBSCRIPTION_METER = TOPIC_METER + "." + SUBSCRIPTION;
    public static final String FILTERED_METER = SUBSCRIPTION_METER + ".filtered";
    public static final String SUBSCRIPTION_BATCH_METER = TOPIC_METER + "." + SUBSCRIPTION + ".batch";
    public static final String FAILED_METER = "failed-meter";
    public static final String FAILED_TOPIC_METER = FAILED_METER + "." + GROUP + "." + TOPIC;
    public static final String FAILED_METER_SUBSCRIPTION = FAILED_TOPIC_METER + "." + SUBSCRIPTION;
    public static final String THROUGHPUT_BYTES = "throughput";
    public static final String TOPIC_THROUGHPUT_BYTES = THROUGHPUT_BYTES + "." + GROUP + "." + TOPIC;
    public static final String SUBSCRIPTION_THROUGHPUT_BYTES = TOPIC_THROUGHPUT_BYTES + "." + SUBSCRIPTION;
    public static final String STATUS_CODES = "http-status-codes.code" + HTTP_CODE;
    public static final String TOPIC_STATUS_CODES = "http-status-codes." + GROUP + "." + TOPIC + ".code" + HTTP_CODE;
    public static final String SUBSCRIPTION_STATUS = "status." + GROUP + "." + TOPIC + "." + SUBSCRIPTION;
    public static final String ERRORS_TIMEOUTS = SUBSCRIPTION_STATUS + ".errors.timeout";
    public static final String ERRORS_OTHER = SUBSCRIPTION_STATUS + ".errors.other";
    public static final String ERRORS_HTTP_BY_FAMILY = SUBSCRIPTION_STATUS + "." + HTTP_CODE_FAMILY;
    public static final String ERRORS_HTTP_BY_CODE = ERRORS_HTTP_BY_FAMILY + "." + HTTP_CODE;
    public static final String DISCARDED_METER = "discarded-meter";
    public static final String DISCARDED_TOPIC_METER = DISCARDED_METER + "." + GROUP + "." + TOPIC;
    public static final String DISCARDED_SUBSCRIPTION_METER = DISCARDED_TOPIC_METER + "." + SUBSCRIPTION;

    public static final String DELAYED_PROCESSING = "delayed-processing";
    public static final String TOPIC_DELAYED_PROCESSING = DELAYED_PROCESSING + "." + GROUP + "." + TOPIC;

    public static final String OAUTH_SUBSCRIPTION_TOKEN_REQUEST = "oauth.subscription." + GROUP + "." + TOPIC + "." + SUBSCRIPTION
            + ".token-request." + OAUTH_PROVIDER_NAME;

    public static final String PERSISTED_UNDELIVERED_MESSAGES_METER = "undelivered-messages.persisted";
}
