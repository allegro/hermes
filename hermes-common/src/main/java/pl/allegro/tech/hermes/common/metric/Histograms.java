package pl.allegro.tech.hermes.common.metric;

import static pl.allegro.tech.hermes.metrics.PathsCompiler.*;

public class Histograms {
    public static final String MESSAGE_SIZE = "message-size." + GROUP + "." + TOPIC,
            GLOBAL_MESSAGE_SIZE = "message-size",
            INFLIGHT_TIME = "inflight." + GROUP + "." + TOPIC + "." + SUBSCRIPTION + ".time",
            CONSUMERS_WORKLOAD_SELECTIVE_MISSING_RESOURCES = "consumers-workload." + KAFKA_CLUSTER + ".selective.missing-resources",
            CONSUMERS_WORKLOAD_SELECTIVE_DELETED_ASSIGNMENTS = "consumers-workload." + KAFKA_CLUSTER + ".selective.deleted-assignments",
            CONSUMERS_WORKLOAD_SELECTIVE_CREATED_ASSIGNMENTS = "consumers-workload." + KAFKA_CLUSTER + ".selective.created-assignments";
}
