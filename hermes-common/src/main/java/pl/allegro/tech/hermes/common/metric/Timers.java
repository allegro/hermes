package pl.allegro.tech.hermes.common.metric;

import static pl.allegro.tech.hermes.metrics.PathsCompiler.EXECUTOR_NAME;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.GROUP;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.KAFKA_CLUSTER;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.SUBSCRIPTION;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.TOPIC;

public class Timers {

    public static final String
            ACK_ALL_BROKER_LATENCY = "ack-all.broker-latency",
            ACK_ALL_BROKER_TOPIC_LATENCY = ACK_ALL_BROKER_LATENCY + "." + GROUP + "." + TOPIC,

            ACK_LEADER_BROKER_LATENCY = "ack-leader.broker-latency",
            ACK_LEADER_BROKER_TOPIC_LATENCY = ACK_LEADER_BROKER_LATENCY + "." + GROUP + "." + TOPIC,

            PARSING_REQUEST = "parsing-request",
            TOPIC_PARSING_REQUEST = PARSING_REQUEST + "." + GROUP + "." + TOPIC,

            ACK_ALL_LATENCY = "ack-all.latency",
            ACK_ALL_TOPIC_LATENCY = ACK_ALL_LATENCY + "." + GROUP + "." + TOPIC,

            ACK_LEADER_LATENCY = "ack-leader.latency",
            PRODUCER_ACK_LEADER_TOPIC_LATENCY = ACK_LEADER_LATENCY + "." + GROUP + "." + TOPIC,

            VALIDATION_LATENCY = "validation-latency",
            VALIDATION_TOPIC_LATENCY = VALIDATION_LATENCY + "." + TOPIC,

            LATENCY = "latency",
            SUBSCRIPTION_LATENCY = LATENCY + "." + GROUP + "." + TOPIC + "." + SUBSCRIPTION,

            READ_LATENCY = "read-latency",

            CONSUMER_WORKLOAD_REBALANCE_DURATION = "consumers-workload." + KAFKA_CLUSTER + ".selective.rebalance-duration",

            EXECUTOR_DURATION = "executors." + EXECUTOR_NAME + ".duration",
            EXECUTOR_WAITING = "executors." + EXECUTOR_NAME + ".waiting";
}