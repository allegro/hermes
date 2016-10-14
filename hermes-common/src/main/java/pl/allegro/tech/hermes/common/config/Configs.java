package pl.allegro.tech.hermes.common.config;

import com.google.common.io.Files;
import pl.allegro.tech.hermes.common.util.InetAddressHostnameResolver;

import static java.lang.Math.abs;
import static java.util.UUID.randomUUID;

@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
public enum Configs {

    ZOOKEEPER_CONNECT_STRING("zookeeper.connect.string", "localhost:2181"),
    ZOOKEEPER_BASE_SLEEP_TIME("zookeeper.base.sleep.time", 1000),
    ZOOKEEPER_CONNECTION_TIMEOUT("zookeeper.connection.timeout", 10000),
    ZOOKEEPER_SESSION_TIMEOUT("zookeeper.session.timeout", 10000),

    ZOOKEEPER_AUTHORIZATION_ENABLED("zookeeper.authorization.enabled", false),
    ZOOKEEPER_AUTHORIZATION_SCHEME("zookeeper.authorization.scheme", "digest"),
    ZOOKEEPER_AUTHORIZATION_USER("zookeeper.authorization.user", "user"),
    ZOOKEEPER_AUTHORIZATION_PASSWORD("zookeeper.authorization.password", "password"),

    ZOOKEEPER_MAX_RETRIES("zookeeper.max.retries", 2),
    ZOOKEEPER_ROOT("zookeeper.root", "/hermes"),
    ZOOKEEPER_CACHE_THREAD_POOL_SIZE("zookeeper.cache.thread.pool.size", 5),
    ZOOKEEPER_TASK_PROCESSING_THREAD_POOL_SIZE("zookeeper.cache.processing.thread.pool.size", 5),

    KAFKA_ZOOKEEPER_CONNECT_STRING("kafka.zookeeper.connect.string", "localhost:2181"),

    ENVIRONMENT_NAME("environment.name", "dev"),
    HOSTNAME("hostname", new InetAddressHostnameResolver().resolve()),

    KAFKA_CLUSTER_NAME("kafka.cluster.name", "primary"),
    KAFKA_BROKER_LIST("kafka.broker.list", "localhost:9092"),
    KAFKA_NAMESPACE("kafka.namespace", ""),

    KAFKA_CONSUMER_AUTO_OFFSET_RESET_CONFIG("kafka.consumer.auto.offset.reset", "latest"),
    KAFKA_CONSUMER_SESSION_TIMEOUT_MS_CONFIG("kafka.consumer.session.timeout.ms", 200_000),
    KAFKA_CONSUMER_HEARTBEAT_INTERVAL_MS_CONFIG("kafka.consumer.heartbeat.interval.ms", 3000),
    KAFKA_CONSUMER_METADATA_MAX_AGE_CONFIG("kafka.consumer.metadata.max.age.ms", 5 * 60 * 1000),
    KAFKA_CONSUMER_MAX_PARTITION_FETCH_BYTES_CONFIG("kafka.consumer.max.partition.fetch.bytes", 10 * 1024 * 1024),
    KAFKA_CONSUMER_SEND_BUFFER_CONFIG("kafka.consumer.send.buffer.bytes", 256 * 1024),
    KAFKA_CONSUMER_RECEIVE_BUFFER_CONFIG("kafka.consumer.receive.buffer.bytes", 256 * 1024),
    KAFKA_CONSUMER_FETCH_MIN_BYTES_CONFIG("kafka.consumer.fetch.min.bytes", 1),
    KAFKA_CONSUMER_FETCH_MAX_WAIT_MS_CONFIG("kafka.consumer.fetch.max.wait.ms", 500),
    KAFKA_CONSUMER_RECONNECT_BACKOFF_MS_CONFIG("kafka.consumer.reconnect.backoff.ms", 500),
    KAFKA_CONSUMER_RETRY_BACKOFF_MS_CONFIG("kafka.consumer.retry.backoff.ms", 500),
    KAFKA_CONSUMER_CHECK_CRCS_CONFIG("kafka.consumer.check.crcs", true),
    KAFKA_CONSUMER_METRICS_SAMPLE_WINDOW_MS_CONFIG("kafka.consumer.metrics.sample.window.ms", 30000),
    KAFKA_CONSUMER_METRICS_NUM_SAMPLES_CONFIG("kafka.consumer.metrics.num.samples", 2),
    KAFKA_CONSUMER_REQUEST_TIMEOUT_MS_CONFIG("kafka.consumer.request.timeout.ms", 250_000),
    KAFKA_CONSUMER_CONNECTIONS_MAX_IDLE_MS_CONFIG("kafka.consumer.connections.max.idle.ms", 9 * 60 * 1000),
    KAFKA_CONSUMER_MAX_POLL_RECORDS_CONFIG("kafka.consumer.max.poll.records", 10),

    KAFKA_SIMPLE_CONSUMER_TIMEOUT_MS("kafka.simple.consumer.timeout.ms", 5000),
    KAFKA_SIMPLE_CONSUMER_BUFFER_SIZE("kafka.simple.consumer.buffer.size", 64 * 1024),
    KAFKA_SIMPLE_CONSUMER_ID_PREFIX("kafka.simple.consumer.id.prefix", "offsetChecker"),
    KAFKA_SIMPLE_CONSUMER_CACHE_EXPIRATION_IN_SECONDS("kafka.simple.consumer.cache.expiration.in.seconds", 60),

    KAFKA_PRODUCER_METADATA_FETCH_TIMEOUT_MS("kafka.producer.metadata.fetch.timeout.ms", 500),
    KAFKA_PRODUCER_METADATA_MAX_AGE("kafka.producer.metadata.max.age.ms", 5 * 60 * 1000),
    KAFKA_PRODUCER_COMPRESSION_CODEC("kafka.producer.compression.codec", "none"),
    KAFKA_PRODUCER_RETRIES("kafka.producer.retries", Integer.MAX_VALUE),
    KAFKA_PRODUCER_BUFFER_MEMORY("kafka.producer.buffer.memory", 256 * 1024 * 1024L),
    KAFKA_PRODUCER_RETRY_BACKOFF_MS("kafka.producer.retry.backoff.ms", 256),
    KAFKA_PRODUCER_BLOCK_ON_BUFFER_FULL("kafka.producer.block.on.buffer.full", false),
    KAFKA_PRODUCER_ACK_TIMEOUT("kafka.producer.ack.timeout", 1000),
    KAFKA_PRODUCER_BATCH_SIZE("kafka.producer.batch.size", 16 * 1024),
    KAFKA_PRODUCER_TCP_SEND_BUFFER("kafka.producer.tcp.send.buffer", 128 * 1024),
    KAFKA_PRODUCER_MAX_REQUEST_SIZE("kafka.producer.max.request.size", 1024 * 1024),
    KAFKA_PRODUCER_LINGER_MS("kafka.producer.linger.ms", 0),
    KAFKA_PRODUCER_METRICS_SAMPLE_WINDOW_MS("kafka.producer.metrics.sample.window.ms", 30000),
    KAFKA_PRODUCER_MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION("kafka.producer.max.in.flight.requests.per.connection", 5),
    KAFKA_PRODUCER_REPORT_NODE_METRICS("kafka.producer.report.node.metrics", false),
    KAFKA_STREAM_COUNT("kafka.stream.count", 1),

    FRONTEND_PORT("frontend.port", 8080),
    FRONTEND_HOST("frontend.host", "0.0.0.0"),
    FRONTEND_IDLE_TIMEOUT("frontend.idle.timeout", 65),
    FRONTEND_LONG_IDLE_TIMEOUT("frontend.long.idle.timeout", 400),
    FRONTEND_READ_TIMEOUT("frontend.read.timeout", 2000),
    FRONTEND_REQUEST_PARSE_TIMEOUT("frontend.request.parse.timeout", 5000),
    FRONTEND_MAX_HEADERS("frontend.max.headers", 20),
    FRONTEND_MAX_PARAMETERS("frontend.max.parameters", 10),
    FRONTEND_MAX_COOKIES("frontend.max.cookies", 10),
    FRONTEND_BACKLOG_SIZE("frontend.backlog.size", 10000),
    FRONTEND_IO_THREADS_COUNT("frontend.io.threads.count", Runtime.getRuntime().availableProcessors() * 2),
    FRONTEND_WORKER_THREADS_COUNT("frontend.worker.threads.count", 200),
    FRONTEND_SET_KEEP_ALIVE("frontend.keepalive.set", false),
    FRONTEND_REQUEST_DUMPER("frontend.request.dumper", false),
    FRONTEND_BUFFER_SIZE("frontend.buffer.size", 16384),
    FRONTEND_REQUEST_CHUNK_SIZE("frontend.request.chunk.size", 1024),
    FRONTEND_GRACEFUL_SHUTDOWN_ENABLED("frontend.graceful.shutdown.enabled", true),
    FRONTEND_GRACEFUL_SHUTDOWN_INITIAL_WAIT_MS("frontend.graceful.shutdown.initial.wait.ms", 10000),
    FRONTEND_HTTP2_ENABLED("frontend.http2.enabled", false),

    FRONTEND_SSL_ENABLED("frontend.ssl.enabled", false),
    FRONTEND_SSL_PORT("frontend.ssl.port", 8443),
    FRONTEND_SSL_PROTOCOL("frontend.ssl.protocol", "TLS"),
    FRONTEND_SSL_KEYSTORE_LOCATION("frontend.ssl.keystore.location", "classpath:server.keystore"),
    FRONTEND_SSL_KEYSTORE_PASSWORD("frontend.ssl.keystore.password", "password"),
    FRONTEND_SSL_KEYSTORE_FORMAT("frontend.ssl.keystore.format", "JKS"),
    FRONTEND_SSL_TRUSTSTORE_LOCATION("frontend.ssl.truststore.location", "classpath:server.truststore"),
    FRONTEND_SSL_TRUSTSTORE_PASSWORD("frontend.ssl.truststore.password", "password"),
    FRONTEND_SSL_TRUSTSTORE_FORMAT("frontend.ssl.truststore.format", "JKS"),

    FRONTEND_MESSAGE_PREVIEW_ENABLED("frontend.message.preview.enabled", false),
    FRONTEND_MESSAGE_PREVIEW_SIZE("frontend.message.preview.size", 3),
    FRONTEND_MESSAGE_PREVIEW_LOG_PERSIST_PERIOD("frontend.message.preview.log.persist.period.seconds", 30),

    MESSAGES_LOCAL_STORAGE_ENABLED("frontend.messages.local.storage.enabled", false),
    MESSAGES_LOCAL_STORAGE_DIRECTORY("frontend.messages.local.storage.directory", Files.createTempDir().getAbsolutePath()),
    MESSAGES_LOCAL_STORAGE_MAX_AGE_HOURS("frontend.messages.local.storage.max.age.hours", 72),
    MESSAGES_LOCAL_STORAGE_MAX_RESEND_RETRIES("frontend.messages.local.storage.max.resend.retries", 5),
    MESSAGES_LOADING_WAIT_FOR_TOPICS_CACHE("frontend.messages.loading.wait.for.topics.cache", 10),
    MESSAGES_LOADING_WAIT_FOR_BROKER_TOPIC_INFO("frontend.messages.loading.wait.for.broker.topic.info", 5),

    CONSUMER_RECEIVER_POOL_TIMEOUT("consumer.receiver.pool.timeout", 100),
    CONSUMER_RECEIVER_READ_QUEUE_CAPACITY("consumer.receiver.read.queue.capacity", 1000),

    CONSUMER_COMMIT_OFFSET_PERIOD("consumer.commit.offset.period", 60),
    CONSUMER_COMMIT_OFFSET_QUEUES_SIZE("consumer.commit.offset.queues.size", 200_000),

    CONSUMER_SENDER_ASYNC_TIMEOUT_MS("consumer.sender.async.timeout.ms", 5_000),
    CONSUMER_SENDER_ASYNC_TIMEOUT_THREAD_POOL_SIZE("consumer.sender.async.timeout.thread.pool.size", 32),
    CONSUMER_SENDER_ASYNC_TIMEOUT_THREAD_POOL_MONITORING("consumer.sender.async.timeout.thread.pool.monitoring", false),
    CONSUMER_THREAD_POOL_SIZE("consumer.thread.pool.size", 500),
    CONSUMER_HTTP_CLIENT_THREAD_POOL_SIZE("consumer.http.client.thread.pool.size", 30),
    CONSUMER_HTTP_CLIENT_THREAD_POOL_MONITORING("consumer.http.client.thread.pool.monitoring", false),
    CONSUMER_HTTP_CLIENT_MAX_CONNECTIONS_PER_DESTINATION("consumer.http.client.max.connections.per.destination", 100),

    CONSUMER_INFLIGHT_SIZE("consumer.inflight.size", 100),
    CONSUMER_RATE_LIMITER_SUPERVISOR_PERIOD("consumer.rate.limiter.supervisor.period", 30),
    CONSUMER_RATE_LIMITER_REPORTING_THREAD_POOL_SIZE("consumer.rate.limiter.reporting.thread.pool.size", 30),
    CONSUMER_RATE_LIMITER_REPORTING_THREAD_POOL_MONITORING("consumer.rate.limiter.reporting.thread.pool.monitoring", false),
    CONSUMER_RATE_LIMITER_HEARTBEAT_MODE_DELAY("consumer.rate.limiter.hearbeat.mode.delay", 60),
    CONSUMER_RATE_LIMITER_SLOW_MODE_DELAY("consumer.rate.limiter.slow.mode.delay", 1),
    CONSUMER_RATE_CONVERGENCE_FACTOR("consumer.rate.convergence.factor", 0.2),
    CONSUMER_RATE_FAILURES_NOCHANGE_TOLERANCE_RATIO("consumer.rate.failures.nochange.tolerance.ratio", 0.05),
    CONSUMER_RATE_FAILURES_SPEEDUP_TOLERANCE_RATIO("consumer.rate.failures.speedup.tolerance.ratio", 0.01),
    CONSUMER_HEALTH_CHECK_PORT("consumer.status.health.port", 8000),
    CONSUMER_WORKLOAD_ALGORITHM("consumer.workload.algorithm", "mirror"),
    CONSUMER_WORKLOAD_REBALANCE_INTERVAL("consumer.workload.rebalance.interval.seconds", 30),
    CONSUMER_WORKLOAD_CONSUMERS_PER_SUBSCRIPTION("consumer.workload.consumers.per.subscription", 2),
    CONSUMER_WORKLOAD_MAX_SUBSCRIPTIONS_PER_CONSUMER("consumer.workload.max.subscriptions.per.consumer", 200),
    CONSUMER_WORKLOAD_ASSIGNMENT_PROCESSING_THREAD_POOL_SIZE("consumer.workload.assignment.processing.thread.pool.size", 5),
    CONSUMER_WORKLOAD_NODE_ID("consumer.workload.node.id",
            new InetAddressHostnameResolver().resolve().replaceAll("\\.", "_") + "$" + abs(randomUUID().getMostSignificantBits())),
    CONSUMER_WORKLOAD_MONITOR_SCAN_INTERVAL("consumer.workload.monitor.scan.interval.seconds", 120),
    CONSUMER_BATCH_POOLABLE_SIZE("consumer.batch.poolable.size", 1024),
    CONSUMER_BATCH_MAX_POOL_SIZE("consumer.batch.max.pool.size", 64*1024*1024),
    CONSUMER_BATCH_CONNECTION_TIMEOUT("consumer.batch.connection.timeout", 500),
    CONSUMER_BATCH_SOCKET_TIMEOUT("consumer.batch.socket.timeout", 500),
    CONSUMER_FILTERING_ENABLED("consumer.filtering.enabled", true),

    CONSUMER_BACKGROUND_SUPERVISOR_INTERVAL("consumer.supervisor.background.interval", 20_000),
    CONSUMER_BACKGROUND_SUPERVISOR_UNHEALTHY_AFTER("consumer.supervisor.background.unhealty.after", 600_000),
    CONSUMER_BACKGROUND_SUPERVISOR_KILL_AFTER("consumer.supervisor.background.kill.after", 300_000),
    CONSUMER_SIGNAL_PROCESSING_INTERVAL("consumer.supervisor.signal.processing.interval.ms", 5_000),
    CONSUMER_SIGNAL_PROCESSING_QUEUE_SIZE("consumer.supervisor.signal.queue.size", 5_000),

    CONSUMER_CLIENT_ID("consumer.clientId", new InetAddressHostnameResolver().resolve()),

    OAUTH_MISSING_SUBSCRIPTION_HANDLERS_CREATION_DELAY("oauth.missing.subscription.handlers.creation.delay", 10_000L),
    OAUTH_SUBSCRIPTION_TOKENS_CACHE_MAX_SIZE("oauth.subscription.tokens.cache.max.size", 1000L),
    OAUTH_PROVIDERS_TOKEN_REQUEST_RATE_LIMITER_RATE_REDUCTION_FACTOR(
            "oauth.providers.token.request.rate.limiter.rate.reduction.factor", 2.0),

    GRAPHITE_HOST("graphite.host", "localhost"),
    GRAPHITE_PORT("graphite.port", 2003),
    GRAPHITE_HTTP_PORT("graphite.http.port", 8082),
    REPORT_PERIOD("report.period", 20),

    METRICS_REGISTRY_NAME("metrics.registry.name", null),
    METRICS_ZOOKEEPER_REPORTER("metrics.zookeeper.reporter", true),
    METRICS_GRAPHITE_REPORTER("metrics.graphite.reporter", false),
    METRICS_CONSOLE_REPORTER("metrics.console.reporter", false),
    METRICS_COUNTER_EXPIRE_AFTER_ACCESS("metrics.counter.expire.after.access", 72),

    ADMIN_REAPER_INTERAL_MS("admin.reaper.interval.ms", 30000),

    MESSAGE_CONTENT_ROOT("message.content.root", "message"),
    METADATA_CONTENT_ROOT("metadata.content.root", "metadata"),
    GRAPHITE_PREFIX("graphite.prefix", "stats.tech.hermes"),

    SCHEMA_CACHE_REFRESH_AFTER_WRITE_MINUTES("schema.cache.refresh.after.write.minutes", 10),
    SCHEMA_CACHE_EXPIRE_AFTER_WRITE_MINUTES("schema.cache.expire.after.write.minutes", 60 * 24),
    SCHEMA_CACHE_COMPILED_EXPIRE_AFTER_ACCESS_MINUTES("schema.cache.compiled.expire.after.access.minutes", 60 * 48),
    SCHEMA_CACHE_RELOAD_THREAD_POOL_SIZE("schema.cache.reload.thread.pool.size", 2),
    SCHEMA_CACHE_ENABLED("schema.cache.enabled", true),
    SCHEMA_CACHE_COMPILED_MAXIMUM_SIZE("schema.cache.compiled.maximum.size", 2000),
    SCHEMA_REPOSITORY_TYPE("schema.repository.type", "zookeeper"),
    SCHEMA_REPOSITORY_SERVER_URL("schema.repository.serverUrl", "http://localhost:8888/schema-repo/"),
    SCHEMA_REPOSITORY_HTTP_READ_TIMEOUT_MS("schema.repository.http.read.timeout.ms", 2000),
    SCHEMA_REPOSITORY_HTTP_CONNECT_TIMEOUT_MS("schema.repository.http.connect.timeout.ms", 2000),

    UNDELIVERED_MESSAGE_LOG_PERSIST_PERIOD_MS("undelivered.message.log.persist.period.ms", 5000);

    private final String name;

    private final Object defaultValue;

    Configs(String name, Object defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return name;
    }

    @SuppressWarnings("unchecked")
    public <T> T getDefaultValue() {
        return (T) defaultValue;
    }
}
