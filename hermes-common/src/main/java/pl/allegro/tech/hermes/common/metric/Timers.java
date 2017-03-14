package pl.allegro.tech.hermes.common.metric;

import static pl.allegro.tech.hermes.metrics.PathsCompiler.EXECUTOR_NAME;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.GROUP;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.KAFKA_CLUSTER;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.OAUTH_PROVIDER_NAME;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.SUBSCRIPTION;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.TOPIC;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.SCHEMA_REPO_TYPE;

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
            ACK_LEADER_TOPIC_LATENCY = ACK_LEADER_LATENCY + "." + GROUP + "." + TOPIC,

            MESSAGE_CREATION_LATENCY = "message-creation-latency",
            MESSAGE_CREATION_TOPIC_LATENCY = MESSAGE_CREATION_LATENCY + "." + TOPIC,

            LATENCY = "latency",
            SUBSCRIPTION_LATENCY = LATENCY + "." + GROUP + "." + TOPIC + "." + SUBSCRIPTION,

            READ_LATENCY = "read-latency",

            SCHEMA = "schema." + SCHEMA_REPO_TYPE,
            SCHEMA_READ_LATENCY = SCHEMA + ".read-schema",
            SCHEMA_VERSIONS_READ_LATENCY = SCHEMA + ".get-schema-versions",

            CONSUMER_WORKLOAD_REBALANCE_DURATION = "consumers-workload." + KAFKA_CLUSTER + ".selective.rebalance-duration",

            OAUTH_PROVIDER_TOKEN_REQUEST_LATENCY = "oauth.provider." + OAUTH_PROVIDER_NAME + ".token-request-latency",

            EXECUTOR_DURATION = "executors." + EXECUTOR_NAME + ".duration",
            EXECUTOR_WAITING = "executors." + EXECUTOR_NAME + ".waiting";
}