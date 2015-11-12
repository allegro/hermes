package pl.allegro.tech.hermes.common.metric;

import static pl.allegro.tech.hermes.metrics.PathsCompiler.*;

public class Histograms {
    public static final String PRODUCER_MESSAGE_SIZE = "producer." + HOSTNAME + ".message-size." + GROUP + "." + TOPIC,
            PRODUCER_GLOBAL_MESSAGE_SIZE = "producer." + HOSTNAME + ".message-size",
            CONSUMER_INFLIGHT_TIME = "consumer." + HOSTNAME + ".inflight." + GROUP + "." + TOPIC + "." + SUBSCRIPTION + ".time",
            CONSUMERS_WORKLOAD_SELECTIVE_MISSING_RESOURCES = "consumers-workload." + KAFKA_CLUSTER + ".selective.missing-resources",
            CONSUMERS_WORKLOAD_SELECTIVE_DELETED_ASSIGNMENTS = "consumers-workload." + KAFKA_CLUSTER + ".selective.deleted-assignments",
            CONSUMERS_WORKLOAD_SELECTIVE_CREATED_ASSIGNMENTS = "consumers-workload." + KAFKA_CLUSTER + ".selective.created-assignments";
}
