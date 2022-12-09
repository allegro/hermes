package pl.allegro.tech.hermes.common.metric;

import static pl.allegro.tech.hermes.metrics.PathsCompiler.GROUP;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.KAFKA_CLUSTER;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.OAUTH_PROVIDER_NAME;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.SCHEMA_REPO_TYPE;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.SUBSCRIPTION;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.TOPIC;

public class Timers {

    public static final String ACK_ALL_BROKER_LATENCY = "ack-all.broker-latency";
    public static final String ACK_LEADER_BROKER_LATENCY = "ack-leader.broker-latency";

    public static final String ACK_ALL_LATENCY = "ack-all.latency";
    public static final String ACK_ALL_TOPIC_LATENCY = ACK_ALL_LATENCY + "." + GROUP + "." + TOPIC;

    public static final String ACK_LEADER_LATENCY = "ack-leader.latency";
    public static final String ACK_LEADER_TOPIC_LATENCY = ACK_LEADER_LATENCY + "." + GROUP + "." + TOPIC;

    public static final String LATENCY = "latency";
    public static final String SUBSCRIPTION_LATENCY = LATENCY + "." + GROUP + "." + TOPIC + "." + SUBSCRIPTION;

    public static final String SCHEMA = "schema." + SCHEMA_REPO_TYPE;
    public static final String GET_SCHEMA_LATENCY = SCHEMA + ".get-schema";
    public static final String GET_SCHEMA_VERSIONS_LATENCY = SCHEMA + ".get-schema-versions";

    public static final String CONSUMER_WORKLOAD_REBALANCE_DURATION = "consumers-workload." + KAFKA_CLUSTER + ".rebalance-duration";
    public static final String CONSUMER_IDLE_TIME = "idle-time." + GROUP + "." + TOPIC + "." + SUBSCRIPTION;

    public static final String OAUTH_PROVIDER_TOKEN_REQUEST_LATENCY = "oauth.provider." + OAUTH_PROVIDER_NAME + ".token-request-latency";
}
