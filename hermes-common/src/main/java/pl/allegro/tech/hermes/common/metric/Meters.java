package pl.allegro.tech.hermes.common.metric;

import static pl.allegro.tech.hermes.metrics.PathsCompiler.*;

public class Meters {

    public static final String
        METER = "meter",
        TOPIC_METER = METER + "." + GROUP + "." + TOPIC,
        SUBSCRIPTION_METER = TOPIC_METER + "." + SUBSCRIPTION,
        SUBSCRIPTION_BATCH_METER = TOPIC_METER + "." + SUBSCRIPTION + ".batch",

        FAILED_METER = "failed-meter",
        FAILED_TOPIC_METER = FAILED_METER + "." + GROUP + "." + TOPIC,

        STATUS_CODES = "http-status-codes.code" + HTTP_CODE,
        TOPIC_STATUS_CODES = "http-status-codes." + GROUP + "." + TOPIC + ".code" + HTTP_CODE,

        ERRORS_TIMEOUTS = "status." + GROUP + "." + TOPIC + "." + SUBSCRIPTION + ".errors.timeout",
        ERRORS_OTHER = "status." + GROUP + "." + TOPIC + "." + SUBSCRIPTION + ".errors.other",
        ERRORS_HTTP_BY_FAMILY = "status." + GROUP + "." + TOPIC + "." + SUBSCRIPTION + "." + HTTP_CODE_FAMILY,

        ERRORS_HTTP_BY_CODE = ERRORS_HTTP_BY_FAMILY + "." + HTTP_CODE,

        FAILED_METER_SUBSCRIPTION = FAILED_METER + "." + SUBSCRIPTION,

        DISCARDED_METER = "discarded-meter",
        DISCARDED_TOPIC_METER = DISCARDED_METER + "." + GROUP + "." + TOPIC,
        DISCARDED_SUBSCRIPTION_METER = DISCARDED_TOPIC_METER + "." + SUBSCRIPTION,

        EXECUTOR_SUBMITTED = "executors." + EXECUTOR_NAME + ".submitted",
        EXECUTOR_COMPLETED = "executors." + EXECUTOR_NAME + ".completed";
}