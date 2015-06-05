package pl.allegro.tech.hermes.common.metric;

import static pl.allegro.tech.hermes.common.metric.PathsCompiler.GROUP;
import static pl.allegro.tech.hermes.common.metric.PathsCompiler.HOSTNAME;
import static pl.allegro.tech.hermes.common.metric.PathsCompiler.SUBSCRIPTION;
import static pl.allegro.tech.hermes.common.metric.PathsCompiler.TOPIC;

public class Timers {

    public static final String

    PRODUCER_ACK_ALL_BROKER_LATENCY = "producer." + HOSTNAME + ".ack-all.broker-latency",
            PRODUCER_ACK_ALL_BROKER_TOPIC_LATENCY = PRODUCER_ACK_ALL_BROKER_LATENCY + "." + GROUP + "." + TOPIC,

    PRODUCER_ACK_LEADER_BROKER_LATENCY = "producer." + HOSTNAME + ".ack-leader.broker-latency",
            PRODUCER_ACK_LEADER_BROKER_TOPIC_LATENCY = PRODUCER_ACK_LEADER_BROKER_LATENCY + "." + GROUP + "." + TOPIC,

    PRODUCER_PARSING_REQUEST = "producer." + HOSTNAME + ".parsing-request",
            PRODUCER_TOPIC_PARSING_REQUEST = PRODUCER_PARSING_REQUEST + "." + GROUP + "." + TOPIC,

    PRODUCER_ACK_ALL_LATENCY = "producer." + HOSTNAME + ".ack-all.latency",
            PRODUCER_ACK_ALL_TOPIC_LATENCY = PRODUCER_ACK_ALL_LATENCY + "." + GROUP + "." + TOPIC,

    PRODUCER_ACK_LEADER_LATENCY = "producer." + HOSTNAME + ".ack-leader.latency",
            PRODUCER_ACK_LEADER_TOPIC_LATENCY = PRODUCER_ACK_LEADER_LATENCY + "." + GROUP + "." + TOPIC,

    PRODUCER_TRACKER_COMMIT_LATENCY = "producer." + HOSTNAME + ".tracker-commit-latency",

    PRODUCER_VALIDATION_LATENCY = "producer." + HOSTNAME + ".validation-latency",
            PRODUCER_VALIDATION_TOPIC_LATENCY = "producer." + HOSTNAME + ".validation-latency." + TOPIC,

    CONSUMER_LATENCY = "consumer." + HOSTNAME + ".latency",
            CONSUMER_SUBSCRIPTION_LATENCY = CONSUMER_LATENCY + "." + GROUP + "." + TOPIC + "." + SUBSCRIPTION,

    CONSUMER_READ_LATENCY = "consumer." + HOSTNAME + ".read-latency",
            CONSUMER_TRACKER_COMMIT_LATENCY = "consumer." + HOSTNAME + ".tracker-commit-latency";
}