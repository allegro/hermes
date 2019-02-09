package pl.allegro.tech.hermes.common.config;

import com.google.common.io.Files;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.util.InetAddressHostnameResolver;

import static java.lang.Math.abs;
import static java.util.UUID.randomUUID;

@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
public enum Configs {

    ZOOKEEPER_CONNECT_STRING("zookeeper.connect.string", "localhost:2181"),
    ZOOKEEPER_BASE_SLEEP_TIME("zookeeper.base.sleep.time", 1000),
    ZOOKEEPER_MAX_SLEEP_TIME_IN_SECONDS("zookeeper.max.sleep.time.seconds", 30),
    ZOOKEEPER_MAX_RETRIES("zookeeper.max.retries", 29),
    ZOOKEEPER_CONNECTION_TIMEOUT("zookeeper.connection.timeout", 10000),
    ZOOKEEPER_SESSION_TIMEOUT("zookeeper.session.timeout", 10000),
    ZOOKEEPER_MAX_INFLIGHT_REQUESTS("zookeeper.max.inflight.requests", 10),

    ZOOKEEPER_AUTHORIZATION_ENABLED("zookeeper.authorization.enabled", false),
    ZOOKEEPER_AUTHORIZATION_SCHEME("zookeeper.authorization.scheme", "digest"),
    ZOOKEEPER_AUTHORIZATION_USER("zookeeper.authorization.user", "user"),
    ZOOKEEPER_AUTHORIZATION_PASSWORD("zookeeper.authorization.password", "password"),

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
    KAFKA_CONSUMER_MAX_PARTITION_FETCH_MIN_BYTES_CONFIG("kafka.consumer.max.partition.fetch.min", Topic.MIN_MESSAGE_SIZE),
    KAFKA_CONSUMER_MAX_PARTITION_FETCH_MAX_BYTES_CONFIG("kafka.consumer.max.partition.fetch.max", Topic.MAX_MESSAGE_SIZE),

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
    KAFKA_CONSUMER_MAX_POLL_RECORDS_CONFIG("kafka.consumer.max.poll.records", 1),
    KAFKA_CONSUMER_MAX_POLL_INTERVAL_CONFIG("kafka.consumer.max.poll.interval.ms", Integer.MAX_VALUE),

    KAFKA_SIMPLE_CONSUMER_TIMEOUT_MS("kafka.simple.consumer.timeout.ms", 5000),
    KAFKA_SIMPLE_CONSUMER_BUFFER_SIZE("kafka.simple.consumer.buffer.size", 64 * 1024),
    KAFKA_SIMPLE_CONSUMER_ID_PREFIX("kafka.simple.consumer.id.prefix", "offsetChecker"),
    KAFKA_SIMPLE_CONSUMER_CACHE_EXPIRATION_IN_SECONDS("kafka.simple.consumer.cache.expiration.in.seconds", 60),

    KAFKA_PRODUCER_MAX_BLOCK_MS("kafka.producer.max.block.ms", 500),
    KAFKA_PRODUCER_METADATA_MAX_AGE("kafka.producer.metadata.max.age.ms", 5 * 60 * 1000),
    KAFKA_PRODUCER_COMPRESSION_CODEC("kafka.producer.compression.codec", "none"),
    KAFKA_PRODUCER_RETRIES("kafka.producer.retries", Integer.MAX_VALUE),
    KAFKA_PRODUCER_RETRY_BACKOFF_MS("kafka.producer.retry.backoff.ms", 256),
    // In the current version of kafka-producer (0.10.1) request.timeout.ms parameter is also used as a timeout
    // for dropping batches from internal accumulator. Therefore, it is better to increase this timeout to very high value,
    // because when kafka is unreachable we don't want to drop messages but buffer them in accumulator until is full.
    // This behavior will change in future version of kafka-producer.
    // More information about this issue:
    // http://mail-archives.apache.org/mod_mbox/kafka-users/201611.mbox/%3C81613078-5734-46FD-82E2-140280758BC6@gmail.com%3E
    KAFKA_PRODUCER_REQUEST_TIMEOUT_MS("kafka.producer.request.timeout.ms", 30 * 60 * 1000),
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
    FRONTEND_ALWAYS_SET_KEEP_ALIVE("frontend.always.keep.alive", false),
    FRONTEND_SET_KEEP_ALIVE("frontend.keep.alive", false),
    FRONTEND_REQUEST_DUMPER("frontend.request.dumper", false),
    FRONTEND_BUFFER_SIZE("frontend.buffer.size", 16384),
    FRONTEND_REQUEST_CHUNK_SIZE("frontend.request.chunk.size", 1024),
    FRONTEND_GRACEFUL_SHUTDOWN_ENABLED("frontend.graceful.shutdown.enabled", true),
    FRONTEND_GRACEFUL_SHUTDOWN_INITIAL_WAIT_MS("frontend.graceful.shutdown.initial.wait.ms", 10000),
    FRONTEND_HTTP2_ENABLED("frontend.http2.enabled", false),
    FRONTEND_FORCE_TOPIC_MAX_MESSAGE_SIZE("frontend.force.topic.max.message.size", false),
    FRONTEND_THROUGHPUT_TYPE("frontend.throughput.type", "unlimited"),
    FRONTEND_THROUGHPUT_FIXED_MAX("frontend.throughput.fixed.max", Long.MAX_VALUE),
    FRONTEND_THROUGHPUT_DYNAMIC_MAX("frontend.throughput.dynamic.max", Long.MAX_VALUE),
    FRONTEND_THROUGHPUT_DYNAMIC_THRESHOLD("frontend.throughput.dynamic.threshold", Long.MAX_VALUE),
    FRONTEND_THROUGHPUT_DYNAMIC_DESIRED("frontend.throughput.dynamic.desired", Long.MAX_VALUE),
    FRONTEND_THROUGHPUT_DYNAMIC_IDLE("frontend.throughput.dynamic.idle", 0.5),
    FRONTEND_THROUGHPUT_DYNAMIC_CHECK_INTERVAL("frontend.throughput.dynamic.interval.seconds", 30),

    FRONTEND_SSL_ENABLED("frontend.ssl.enabled", false),
    FRONTEND_SSL_PORT("frontend.ssl.port", 8443),
    FRONTEND_SSL_CLIENT_AUTH_MODE("frontend.ssl.client.auth.mode", "not_requested"),
    FRONTEND_SSL_PROTOCOL("frontend.ssl.protocol", "TLS"),
    FRONTEND_SSL_KEYSTORE_LOCATION("frontend.ssl.keystore.location", "classpath:server.keystore"),
    FRONTEND_SSL_KEYSTORE_PASSWORD("frontend.ssl.keystore.password", "password"),
    FRONTEND_SSL_KEYSTORE_FORMAT("frontend.ssl.keystore.format", "JKS"),
    FRONTEND_SSL_TRUSTSTORE_LOCATION("frontend.ssl.truststore.location", "classpath:server.truststore"),
    FRONTEND_SSL_TRUSTSTORE_PASSWORD("frontend.ssl.truststore.password", "password"),
    FRONTEND_SSL_TRUSTSTORE_FORMAT("frontend.ssl.truststore.format", "JKS"),

    CONSUMER_SSL_ENABLED("consumer.ssl.enabled", false),
    CONSUMER_SSL_PROTOCOL("consumer.ssl.protocol", "TLS"),
    CONSUMER_SSL_KEYSTORE_LOCATION("consumer.ssl.keystore.location", "classpath:client.keystore"),
    CONSUMER_SSL_KEYSTORE_PASSWORD("consumer.ssl.keystore.password", "password"),
    CONSUMER_SSL_KEYSTORE_FORMAT("consumer.ssl.keystore.format", "JKS"),
    CONSUMER_SSL_TRUSTSTORE_LOCATION("consumer.ssl.truststore.location", "classpath:client.truststore"),
    CONSUMER_SSL_TRUSTSTORE_PASSWORD("consumer.ssl.truststore.password", "password"),
    CONSUMER_SSL_TRUSTSTORE_FORMAT("consumer.ssl.truststore.format", "JKS"),

    FRONTEND_AUTHENTICATION_ENABLED("frontend.authentication.enabled", false),
    FRONTEND_AUTHENTICATION_MODE("frontend.authentication.mode", "constraint_driven"),

    FRONTEND_MESSAGE_PREVIEW_ENABLED("frontend.message.preview.enabled", false),
    FRONTEND_MESSAGE_PREVIEW_MAX_SIZE_KB("frontend.message.preview.max.size.kb", 10),
    FRONTEND_MESSAGE_PREVIEW_SIZE("frontend.message.preview.size", 3),
    FRONTEND_MESSAGE_PREVIEW_LOG_PERSIST_PERIOD("frontend.message.preview.log.persist.period.seconds", 30),

    FRONTEND_STARTUP_TOPIC_METADATA_LOADING_ENABLED("frontend.startup.topic.metadata.loading.enabled", false),
    FRONTEND_STARTUP_TOPIC_METADATA_LOADING_RETRY_INTERVAL("frontend.startup.topic.metadata.loading.retry.interval", 1_000L),
    FRONTEND_STARTUP_TOPIC_METADATA_LOADING_RETRY_COUNT("frontend.startup.topic.metadata.loading.retry.count", 5),
    FRONTEND_STARTUP_TOPIC_METADATA_LOADING_THREAD_POOL_SIZE("frontend.startup.topic.metadata.loading.thread.pool.size", 16),

    FRONTEND_STARTUP_TOPIC_SCHEMA_LOADING_ENABLED("frontend.startup.topic.schema.loading.enabled", false),
    FRONTEND_STARTUP_TOPIC_SCHEMA_LOADING_RETRY_COUNT("frontend.startup.topic.schema.loading.retry.count", 3),
    FRONTEND_STARTUP_TOPIC_SCHEMA_LOADING_THREAD_POOL_SIZE("frontend.startup.topic.schema.loading.thread.pool.size", 16),

    MESSAGES_LOCAL_BUFFERED_STORAGE_SIZE("frontend.messages.local.buffered.storage.size.bytes", 256 * 1024 * 1024L),
    MESSAGES_LOCAL_STORAGE_V2_MIGRATION_ENABLED("frontend.messages.local.storage.v2.migration.enabled", true),
    MESSAGES_LOCAL_STORAGE_ENABLED("frontend.messages.local.storage.enabled", false),
    MESSAGES_LOCAL_STORAGE_DIRECTORY("frontend.messages.local.storage.directory", Files.createTempDir().getAbsolutePath()),
    MESSAGES_LOCAL_STORAGE_TEMPORARY_DIRECTORY("frontend.messages.local.storage.temporary.directory", Files.createTempDir().getAbsolutePath()),
    MESSAGES_LOCAL_STORAGE_AVERAGE_MESSAGE_SIZE("frontend.messages.local.storage.average.message.size.in.bytes", 600),
    MESSAGES_LOCAL_STORAGE_MAX_AGE_HOURS("frontend.messages.local.storage.max.age.hours", 72),
    MESSAGES_LOCAL_STORAGE_MAX_RESEND_RETRIES("frontend.messages.local.storage.max.resend.retries", 5),
    MESSAGES_LOADING_PAUSE_BETWEEN_RESENDS("frontend.messages.loading.pause.between.resend", 30),
    MESSAGES_LOADING_WAIT_FOR_BROKER_TOPIC_INFO("frontend.messages.loading.wait.for.broker.topic.info", 5),
    MESSAGES_LOCAL_STORAGE_SIZE_REPORTING_ENABLED("frontend.messages.local.storage.size.reporting.enabled", true),

    CONSUMER_RECEIVER_POOL_TIMEOUT("consumer.receiver.pool.timeout", 30),
    CONSUMER_RECEIVER_READ_QUEUE_CAPACITY("consumer.receiver.read.queue.capacity", 1000),

    CONSUMER_RECEIVER_WAIT_BETWEEN_UNSUCCESSFUL_POLLS("consumer.receiver.wait.between.unsuccessful.polls", true),
    CONSUMER_RECEIVER_INITIAL_IDLE_TIME("consumer.receiver.initial.idle.time", 10),
    CONSUMER_RECEIVER_MAX_IDLE_TIME("consumer.receiver.max.idle.time", 1000),

    CONSUMER_COMMIT_OFFSET_PERIOD("consumer.commit.offset.period", 60),
    CONSUMER_COMMIT_OFFSET_QUEUES_SIZE("consumer.commit.offset.queues.size", 200_000),

    CONSUMER_SENDER_ASYNC_TIMEOUT_MS("consumer.sender.async.timeout.ms", 5_000),
    CONSUMER_SENDER_ASYNC_TIMEOUT_THREAD_POOL_SIZE("consumer.sender.async.timeout.thread.pool.size", 32),
    CONSUMER_SENDER_ASYNC_TIMEOUT_THREAD_POOL_MONITORING("consumer.sender.async.timeout.thread.pool.monitoring", false),
    CONSUMER_THREAD_POOL_SIZE("consumer.thread.pool.size", 500),
    CONSUMER_HTTP_CLIENT_THREAD_POOL_SIZE("consumer.http.client.thread.pool.size", 30),
    CONSUMER_HTTP_CLIENT_THREAD_POOL_MONITORING("consumer.http.client.thread.pool.monitoring", false),
    CONSUMER_HTTP_CLIENT_MAX_CONNECTIONS_PER_DESTINATION("consumer.http.client.max.connections.per.destination", 100),
    CONSUMER_HTTP_CLIENT_VALIDATE_CERTS("consumer.http.client.validate.certs", true),
    CONSUMER_HTTP_CLIENT_VALIDATE_PEER_CERTS("consumer.http.client.validate.peer.certs", true),
    CONSUMER_HTTP_CLIENT_ENABLE_CRLDP("consumer.http.client.enable.crldp", true),


    CONSUMER_HTTP2_ENABLED("consumer.http2.enabled", true),
    CONSUMER_HTTP2_CLIENT_THREAD_POOL_SIZE("consumer.http2.client.thread.pool.size", 10),
    CONSUMER_HTTP2_CLIENT_THREAD_POOL_MONITORING("consumer.http2.client.thread.pool.monitoring", false),

    CONSUMER_INFLIGHT_SIZE("consumer.inflight.size", 100),
    CONSUMER_RATE_LIMITER_SUPERVISOR_PERIOD("consumer.rate.limiter.supervisor.period", 30),
    CONSUMER_RATE_LIMITER_REPORTING_THREAD_POOL_SIZE("consumer.rate.limiter.reporting.thread.pool.size", 30),
    CONSUMER_RATE_LIMITER_REPORTING_THREAD_POOL_MONITORING("consumer.rate.limiter.reporting.thread.pool.monitoring", false),
    CONSUMER_RATE_LIMITER_HEARTBEAT_MODE_DELAY("consumer.rate.limiter.hearbeat.mode.delay", 60),
    CONSUMER_RATE_LIMITER_SLOW_MODE_DELAY("consumer.rate.limiter.slow.mode.delay", 1),
    CONSUMER_RATE_CONVERGENCE_FACTOR("consumer.rate.convergence.factor", 0.2),
    CONSUMER_RATE_FAILURES_NOCHANGE_TOLERANCE_RATIO("consumer.rate.failures.nochange.tolerance.ratio", 0.05),
    CONSUMER_RATE_FAILURES_SPEEDUP_TOLERANCE_RATIO("consumer.rate.failures.speedup.tolerance.ratio", 0.01),
    CONSUMER_MAXRATE_STRATEGY("consumer.maxrate.strategy", "negotiated"),
    CONSUMER_MAXRATE_BALANCE_INTERVAL_SECONDS("consumer.maxrate.balance.interval.seconds", 30),
    CONSUMER_MAXRATE_UPDATE_INTERVAL_SECONDS("consumer.maxrate.update.interval.seconds", 15),
    CONSUMER_MAXRATE_HISTORY_SIZE("consumer.maxrate.history.size", 1),
    CONSUMER_MAXRATE_BUSY_TOLERANCE("consumer.maxrate.busy.tolerance", 0.1),
    CONSUMER_MAXRATE_MIN_MAX_RATE("consumer.maxrate.min.value", 1.0),
    CONSUMER_MAXRATE_MIN_ALLOWED_CHANGE_PERCENT("consumer.maxrate.min.allowed.change.percent", 1.0),
    CONSUMER_MAXRATE_MIN_SIGNIFICANT_UPDATE_PERCENT("consumer.maxrate.min.significant.update.percent", 9.0),

    CONSUMER_HEALTH_CHECK_PORT("consumer.status.health.port", 8000),
    CONSUMER_WORKLOAD_ALGORITHM("consumer.workload.algorithm", "selective"),
    CONSUMER_WORKLOAD_REBALANCE_INTERVAL("consumer.workload.rebalance.interval.seconds", 30),
    CONSUMER_WORKLOAD_CONSUMERS_PER_SUBSCRIPTION("consumer.workload.consumers.per.subscription", 2),
    CONSUMER_WORKLOAD_MAX_SUBSCRIPTIONS_PER_CONSUMER("consumer.workload.max.subscriptions.per.consumer", 200),
    CONSUMER_WORKLOAD_ASSIGNMENT_PROCESSING_THREAD_POOL_SIZE("consumer.workload.assignment.processing.thread.pool.size", 5),
    CONSUMER_WORKLOAD_NODE_ID("consumer.workload.node.id",
            new InetAddressHostnameResolver().resolve().replaceAll("\\.", "_") + "$" + abs(randomUUID().getMostSignificantBits())),
    CONSUMER_WORKLOAD_MONITOR_SCAN_INTERVAL("consumer.workload.monitor.scan.interval.seconds", 120),
    CONSUMER_WORKLOAD_AUTO_REBALANCE("consumer.workload.rebalance.auto", true),
    CONSUMER_WORKLOAD_DEAD_AFTER_SECONDS("consumer.workload.dead.after.seconds", 120),
    CONSUMER_BATCH_POOLABLE_SIZE("consumer.batch.poolable.size", 1024),
    CONSUMER_BATCH_MAX_POOL_SIZE("consumer.batch.max.pool.size", 64 * 1024 * 1024),
    CONSUMER_BATCH_CONNECTION_TIMEOUT("consumer.batch.connection.timeout", 500),
    CONSUMER_BATCH_SOCKET_TIMEOUT("consumer.batch.socket.timeout", 500),
    CONSUMER_FILTERING_ENABLED("consumer.filtering.enabled", true),

    CONSUMER_BACKGROUND_SUPERVISOR_INTERVAL("consumer.supervisor.background.interval", 20_000),
    CONSUMER_BACKGROUND_SUPERVISOR_UNHEALTHY_AFTER("consumer.supervisor.background.unhealty.after", 600_000),
    CONSUMER_BACKGROUND_SUPERVISOR_KILL_AFTER("consumer.supervisor.background.kill.after", 300_000),
    CONSUMER_SIGNAL_PROCESSING_INTERVAL("consumer.supervisor.signal.processing.interval.ms", 5_000),
    CONSUMER_SIGNAL_PROCESSING_QUEUE_SIZE("consumer.supervisor.signal.queue.size", 5_000),

    CONSUMER_USE_TOPIC_MESSAGE_SIZE("consumer.use.topic.message.size", false),

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

    ADMIN_REAPER_INTERAL_MS("admin.reaper.interval.ms", 180000),
    GLOBAL_SHUTDOWN_HOOK_REGISTERED("global.shutdown.hook.registered", true),

    MESSAGE_CONTENT_ROOT("message.content.root", "message"),
    METADATA_CONTENT_ROOT("metadata.content.root", "metadata"),
    GRAPHITE_PREFIX("graphite.prefix", "stats.tech.hermes"),

    SCHEMA_CACHE_REFRESH_AFTER_WRITE_MINUTES("schema.cache.refresh.after.write.minutes", 10),
    SCHEMA_CACHE_EXPIRE_AFTER_WRITE_MINUTES("schema.cache.expire.after.write.minutes", 60 * 24),
    SCHEMA_CACHE_COMPILED_EXPIRE_AFTER_ACCESS_MINUTES("schema.cache.compiled.expire.after.access.minutes", 60 * 48),
    SCHEMA_CACHE_RELOAD_THREAD_POOL_SIZE("schema.cache.reload.thread.pool.size", 2),
    SCHEMA_CACHE_ENABLED("schema.cache.enabled", true),
    SCHEMA_CACHE_COMPILED_MAXIMUM_SIZE("schema.cache.compiled.maximum.size", 2000),
    SCHEMA_REPOSITORY_TYPE("schema.repository.type", "schema_registry"),
    SCHEMA_REPOSITORY_SERVER_URL("schema.repository.serverUrl", "http://localhost:8888/"),
    SCHEMA_REPOSITORY_HTTP_READ_TIMEOUT_MS("schema.repository.http.read.timeout.ms", 2000),
    SCHEMA_REPOSITORY_HTTP_CONNECT_TIMEOUT_MS("schema.repository.http.connect.timeout.ms", 2000),
    SCHEMA_REPOSITORY_ONLINE_CHECK_PERMITS_PER_SECOND("schema.repository.online.check.permits.per.second", 100.0),
    SCHEMA_REPOSITORY_ONLINE_CHECK_ACQUIRE_WAIT_MS("schema.repository.online.check.acquire.wait.ms", 500),

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
