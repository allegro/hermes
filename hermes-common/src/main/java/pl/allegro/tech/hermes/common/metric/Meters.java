package pl.allegro.tech.hermes.common.metric;

import static pl.allegro.tech.hermes.metrics.PathsCompiler.*;

public class Meters {

    public static final String PRODUCER_METER = "producer." + HOSTNAME + ".meter",
            PRODUCER_TOPIC_METER = PRODUCER_METER + "." + GROUP + "." + TOPIC,

    PRODUCER_FAILED_METER = "producer." + HOSTNAME + ".failed-meter",
            PRODUCER_FAILED_TOPIC_METER = PRODUCER_FAILED_METER + "." + GROUP + "." + TOPIC,

    PRODUCER_STATUS_CODES = "producer." + HOSTNAME + ".http-status-codes.code" + HTTP_CODE,
            PRODUCER_TOPIC_STATUS_CODES = "producer." + HOSTNAME + ".http-status-codes." + GROUP + "." + TOPIC + ".code" + HTTP_CODE,

    CONSUMER_METER = "consumer." + HOSTNAME + ".meter",
            CONSUMER_TOPIC_METER = CONSUMER_METER + "." + GROUP + "." + TOPIC,
            CONSUMER_SUBSCRIPTION_METER = CONSUMER_TOPIC_METER + "." + SUBSCRIPTION,

    CONSUMER_ERRORS_TIMEOUTS = "consumer." + HOSTNAME + ".status." + GROUP + "." + TOPIC + "." + SUBSCRIPTION + ".errors.timeout",

    CONSUMER_ERRORS_OTHER = "consumer." + HOSTNAME + ".status." + GROUP + "." + TOPIC + "." + SUBSCRIPTION + ".errors.other",

    CONSUMER_ERRORS_HTTP_BY_FAMILY = "consumer." + HOSTNAME + ".status." + GROUP + "." + TOPIC + "." + SUBSCRIPTION +
            "." + HTTP_CODE_FAMILY,

    CONSUMER_ERRORS_HTTP_BY_CODE = CONSUMER_ERRORS_HTTP_BY_FAMILY + "." + HTTP_CODE,

    CONSUMER_FAILED_METER = "consumer." + HOSTNAME + ".failed-meter" + "." + SUBSCRIPTION,

    CONSUMER_DISCARDED_METER = "consumer." + HOSTNAME + ".discarded-meter",
            CONSUMER_DISCARDED_TOPIC_METER = CONSUMER_DISCARDED_METER + "." + GROUP + "." + TOPIC,
            CONSUMER_DISCARDED_SUBSCRIPTION_METER = CONSUMER_DISCARDED_TOPIC_METER + "." + SUBSCRIPTION,

    CONSUMER_EXECUTOR_SUBMITTED = "consumer." + HOSTNAME + ".executors." + EXECUTOR_NAME + ".submitted",
    CONSUMER_EXECUTOR_COMPLETED = "consumer." + HOSTNAME + ".executors." + EXECUTOR_NAME + ".completed";
}