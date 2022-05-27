package pl.allegro.tech.hermes.common.config;

import com.google.common.io.Files;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.util.InetAddressInstanceIdResolver;

import java.util.Arrays;

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

    ZOOKEEPER_AUTHORIZATION_ENABLED("zookeeper.authorization.enabled", false),
    ZOOKEEPER_AUTHORIZATION_SCHEME("zookeeper.authorization.scheme", "digest"),
    ZOOKEEPER_AUTHORIZATION_USER("zookeeper.authorization.user", "user"),
    ZOOKEEPER_AUTHORIZATION_PASSWORD("zookeeper.authorization.password", "password"),

    ZOOKEEPER_ROOT("zookeeper.root", "/hermes"),
    ZOOKEEPER_TASK_PROCESSING_THREAD_POOL_SIZE("zookeeper.cache.processing.thread.pool.size", 5),

    HOSTNAME("hostname", new InetAddressInstanceIdResolver().resolve()),

    KAFKA_CLUSTER_NAME("kafka.cluster.name", "primary-dc"),
    KAFKA_BROKER_LIST("kafka.broker.list", "localhost:9092"),
    KAFKA_NAMESPACE("kafka.namespace", ""),
    KAFKA_NAMESPACE_SEPARATOR("kafka.namespace.separator", "_"),

    KAFKA_HEADER_NAME_MESSAGE_ID("kafka.header.name.message.id", "id"),
    KAFKA_HEADER_NAME_TIMESTAMP("kafka.header.name.timestamp", "ts"),
    KAFKA_HEADER_NAME_SCHEMA_VERSION("kafka.header.name.schema.version", "sv"),
    KAFKA_HEADER_NAME_SCHEMA_ID("kafka.header.name.schema.id", "sid"),

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
    KAFKA_ADMIN_REQUEST_TIMEOUT_MS("kafka.admin.request.timeout.ms", 5 * 60 * 1000),

    KAFKA_AUTHORIZATION_ENABLED("kafka.authorization.enabled", false),
    KAFKA_AUTHORIZATION_MECHANISM("kafka.authorization.mechanism", "PLAIN"),
    KAFKA_AUTHORIZATION_PROTOCOL("kafka.authorization.protocol", "SASL_PLAINTEXT"),
    KAFKA_AUTHORIZATION_USERNAME("kafka.authorization.username", "username"),
    KAFKA_AUTHORIZATION_PASSWORD("kafka.authorization.password", "password"),

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

    FRONTEND_KEEP_ALIVE_HEADER_ENABLED("frontend.keep.alive.header.enabled", false),
    FRONTEND_KEEP_ALIVE_HEADER_TIMEOUT_SECONDS("frontend.keep.alive.header.timeout.seconds", 1),

    FRONTEND_SSL_ENABLED("frontend.ssl.enabled", false),
    FRONTEND_SSL_PORT("frontend.ssl.port", 8443),
    FRONTEND_SSL_CLIENT_AUTH_MODE("frontend.ssl.client.auth.mode", "not_requested"),
    FRONTEND_SSL_PROTOCOL("frontend.ssl.protocol", "TLS"),

    FRONTEND_SSL_KEYSTORE_SOURCE("frontend.ssl.keystore.source", "jre"),
    FRONTEND_SSL_KEYSTORE_LOCATION("frontend.ssl.keystore.location", "classpath:server.keystore"),
    FRONTEND_SSL_KEYSTORE_PASSWORD("frontend.ssl.keystore.password", "password"),
    FRONTEND_SSL_KEYSTORE_FORMAT("frontend.ssl.keystore.format", "JKS"),

    FRONTEND_SSL_TRUSTSTORE_SOURCE("frontend.ssl.truststore.source", "jre"),
    FRONTEND_SSL_TRUSTSTORE_LOCATION("frontend.ssl.truststore.location", "classpath:server.truststore"),
    FRONTEND_SSL_TRUSTSTORE_PASSWORD("frontend.ssl.truststore.password", "password"),
    FRONTEND_SSL_TRUSTSTORE_FORMAT("frontend.ssl.truststore.format", "JKS"),

    FRONTEND_AUTHENTICATION_ENABLED("frontend.authentication.enabled", false),
    FRONTEND_AUTHENTICATION_MODE("frontend.authentication.mode", "constraint_driven"),

    FRONTEND_HEADER_PROPAGATION_ENABLED("frontend.header.propagation.enabled", false),
    FRONTEND_HEADER_PROPAGATION_ALLOW_FILTER("frontend.header.propagation.allow.filter", ""),

    FRONTEND_MESSAGE_PREVIEW_ENABLED("frontend.message.preview.enabled", false),
    FRONTEND_MESSAGE_PREVIEW_MAX_SIZE_KB("frontend.message.preview.max.size.kb", 10),
    FRONTEND_MESSAGE_PREVIEW_SIZE("frontend.message.preview.size", 3),
    FRONTEND_MESSAGE_PREVIEW_LOG_PERSIST_PERIOD("frontend.message.preview.log.persist.period.seconds", 30),

    FRONTEND_READINESS_CHECK_ENABLED("frontend.readiness.check.enabled", false),
    FRONTEND_READINESS_CHECK_INTERVAL_SECONDS("frontend.readiness.check.interval.seconds", 1),

    FRONTEND_STARTUP_TOPIC_METADATA_LOADING_ENABLED("frontend.startup.topic.metadata.loading.enabled", false),
    FRONTEND_STARTUP_TOPIC_METADATA_LOADING_RETRY_INTERVAL("frontend.startup.topic.metadata.loading.retry.interval", 1_000L),
    FRONTEND_STARTUP_TOPIC_METADATA_LOADING_RETRY_COUNT("frontend.startup.topic.metadata.loading.retry.count", 5),
    FRONTEND_STARTUP_TOPIC_METADATA_LOADING_THREAD_POOL_SIZE("frontend.startup.topic.metadata.loading.thread.pool.size", 16),

    FRONTEND_TOPIC_METADATA_REFRESH_JOB_ENABLED("frontend.topic.metadata.refresh.job.enabled", true),
    FRONTEND_TOPIC_METADATA_REFRESH_JOB_INTERVAL_SECONDS("frontend.topic.metadata.refresh.job.interval.seconds", 60),

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

    CONSUMER_THREAD_POOL_SIZE("consumer.thread.pool.size", 500),

    CONSUMER_INFLIGHT_SIZE("consumer.inflight.size", 100),
    CONSUMER_FILTERING_RATE_LIMITER_ENABLED("consumer.filtering.rate.limiter.enabled", false),
    CONSUMER_MAXRATE_REGISTRY_TYPE("consumer.maxrate.registry.type", "hierarchical"),
    CONSUMER_MAXRATE_REGISTRY_BINARY_ENCODER_MAX_RATE_BUFFER_SIZE_BYTES("consumer.maxrate.registry.binary.encoder.max.rate.buffer.size.bytes", 100_000),
    CONSUMER_MAXRATE_REGISTRY_BINARY_ENCODER_HISTORY_BUFFER_SIZE_BYTES("consumer.maxrate.registry.binary.encoder.history.buffer.size.bytes", 100_000),
    CONSUMER_MAXRATE_BALANCE_INTERVAL_SECONDS("consumer.maxrate.balance.interval.seconds", 30),
    CONSUMER_MAXRATE_UPDATE_INTERVAL_SECONDS("consumer.maxrate.update.interval.seconds", 15),
    CONSUMER_MAXRATE_HISTORY_SIZE("consumer.maxrate.history.size", 1),
    CONSUMER_MAXRATE_BUSY_TOLERANCE("consumer.maxrate.busy.tolerance", 0.1),
    CONSUMER_MAXRATE_MIN_MAX_RATE("consumer.maxrate.min.value", 1.0),
    CONSUMER_MAXRATE_MIN_ALLOWED_CHANGE_PERCENT("consumer.maxrate.min.allowed.change.percent", 1.0),
    CONSUMER_MAXRATE_MIN_SIGNIFICANT_UPDATE_PERCENT("consumer.maxrate.min.significant.update.percent", 9.0),

    CONSUMER_HEALTH_CHECK_PORT("consumer.status.health.port", 8000),
    CONSUMER_WORKLOAD_REGISTRY_TYPE("consumer.workload.registry.type", "hierarchical"),
    CONSUMER_WORKLOAD_REGISTRY_BINARY_ENCODER_ASSIGNMENTS_BUFFER_SIZE_BYTES("consumer.workload.registry.binary.encoder.assignments.buffer.size.bytes", 100_000),
    CONSUMER_WORKLOAD_REBALANCE_INTERVAL("consumer.workload.rebalance.interval.seconds", 30),
    CONSUMER_WORKLOAD_CONSUMERS_PER_SUBSCRIPTION("consumer.workload.consumers.per.subscription", 2),
    CONSUMER_WORKLOAD_MAX_SUBSCRIPTIONS_PER_CONSUMER("consumer.workload.max.subscriptions.per.consumer", 200),
    CONSUMER_WORKLOAD_ASSIGNMENT_PROCESSING_THREAD_POOL_SIZE("consumer.workload.assignment.processing.thread.pool.size", 5),
    CONSUMER_WORKLOAD_NODE_ID("consumer.workload.node.id",
            new InetAddressInstanceIdResolver().resolve().replaceAll("\\.", "_") + "$" + abs(randomUUID().getMostSignificantBits())),
    CONSUMER_WORKLOAD_MONITOR_SCAN_INTERVAL("consumer.workload.monitor.scan.interval.seconds", 120),
    CONSUMER_WORKLOAD_AUTO_REBALANCE("consumer.workload.rebalance.auto", true),
    CONSUMER_WORKLOAD_DEAD_AFTER_SECONDS("consumer.workload.dead.after.seconds", 120),
    CONSUMER_BATCH_POOLABLE_SIZE("consumer.batch.poolable.size", 1024),
    CONSUMER_BATCH_MAX_POOL_SIZE("consumer.batch.max.pool.size", 64 * 1024 * 1024),
    CONSUMER_BATCH_CONNECTION_TIMEOUT("consumer.batch.connection.timeout", 500),
    CONSUMER_BATCH_CONNECTION_REQUEST_TIMEOUT("consumer.batch.connection.request.timeout", 500),
    CONSUMER_FILTERING_ENABLED("consumer.filtering.enabled", true),
    CONSUMER_SUBSCRIPTION_IDS_CACHE_REMOVED_EXPIRE_AFTER_ACCESS_SECONDS("consumer.subscription.ids.cache.removed.expire.after.access.seconds", 60L),

    CONSUMER_BACKGROUND_SUPERVISOR_INTERVAL("consumer.supervisor.background.interval", 20_000),
    CONSUMER_BACKGROUND_SUPERVISOR_UNHEALTHY_AFTER("consumer.supervisor.background.unhealty.after", 600_000),
    CONSUMER_BACKGROUND_SUPERVISOR_KILL_AFTER("consumer.supervisor.background.kill.after", 300_000),
    CONSUMER_SIGNAL_PROCESSING_INTERVAL("consumer.supervisor.signal.processing.interval.ms", 5_000),
    CONSUMER_SIGNAL_PROCESSING_QUEUE_SIZE("consumer.supervisor.signal.queue.size", 5_000),

    CONSUMER_USE_TOPIC_MESSAGE_SIZE("consumer.use.topic.message.size", false),

    CONSUMER_CLIENT_ID("consumer.clientId", new InetAddressInstanceIdResolver().resolve()),

    OAUTH_MISSING_SUBSCRIPTION_HANDLERS_CREATION_DELAY("oauth.missing.subscription.handlers.creation.delay", 10_000L),
    OAUTH_SUBSCRIPTION_TOKENS_CACHE_MAX_SIZE("oauth.subscription.tokens.cache.max.size", 1000L),
    OAUTH_PROVIDERS_TOKEN_REQUEST_RATE_LIMITER_RATE_REDUCTION_FACTOR(
            "oauth.providers.token.request.rate.limiter.rate.reduction.factor", 2.0),

    GRAPHITE_HOST("graphite.host", "localhost"),
    GRAPHITE_PORT("graphite.port", 2003),
    GRAPHITE_HTTP_PORT("graphite.http.port", 8082),
    REPORT_PERIOD("report.period", 20),

    METRICS_ZOOKEEPER_REPORTER("metrics.zookeeper.reporter", true),
    METRICS_GRAPHITE_REPORTER("metrics.graphite.reporter", false),
    METRICS_CONSOLE_REPORTER("metrics.console.reporter", false),
    METRICS_COUNTER_EXPIRE_AFTER_ACCESS("metrics.counter.expire.after.access", 72),
    METRICS_RESERVOIR_TYPE("metrics.reservoir.type", "exponentially_decaying"),

    METRICS_DISABLED_ATTRIBUTES("metrics.disabled.attributes", "M15_RATE, M5_RATE, MEAN, MEAN_RATE, MIN, STDDEV"),

    MESSAGE_CONTENT_ROOT("message.content.root", "message"),
    METADATA_CONTENT_ROOT("metadata.content.root", "metadata"),
    GRAPHITE_PREFIX("graphite.prefix", "stats.tech.hermes"),

    SCHEMA_CACHE_REFRESH_AFTER_WRITE_MINUTES("schema.cache.refresh.after.write.minutes", 10),
    SCHEMA_CACHE_EXPIRE_AFTER_WRITE_MINUTES("schema.cache.expire.after.write.minutes", 60 * 24),
    SCHEMA_CACHE_COMPILED_EXPIRE_AFTER_ACCESS_MINUTES("schema.cache.compiled.expire.after.access.minutes", 60 * 48),
    SCHEMA_CACHE_RELOAD_THREAD_POOL_SIZE("schema.cache.reload.thread.pool.size", 2),
    SCHEMA_CACHE_ENABLED("schema.cache.enabled", true),
    SCHEMA_CACHE_COMPILED_MAXIMUM_SIZE("schema.cache.compiled.maximum.size", 2000),
    SCHEMA_REPOSITORY_SERVER_URL("schema.repository.serverUrl", "http://localhost:8888/"),
    SCHEMA_REPOSITORY_HTTP_READ_TIMEOUT_MS("schema.repository.http.read.timeout.ms", 2000),
    SCHEMA_REPOSITORY_HTTP_CONNECT_TIMEOUT_MS("schema.repository.http.connect.timeout.ms", 2000),
    SCHEMA_REPOSITORY_ONLINE_CHECK_PERMITS_PER_SECOND("schema.repository.online.check.permits.per.second", 100.0),
    SCHEMA_REPOSITORY_ONLINE_CHECK_ACQUIRE_WAIT_MS("schema.repository.online.check.acquire.wait.ms", 500),
    SCHEMA_REPOSITORY_SUBJECT_SUFFIX_ENABLED("schema.repository.subject.suffix.enabled", false),
    SCHEMA_REPOSITORY_SUBJECT_NAMESPACE_ENABLED("schema.repository.subject.namespace.enabled", false),
    SCHEMA_ID_HEADER_ENABLED ("schema.id.header.enabled", false),
    SCHEMA_ID_SERIALIZATION_ENABLED("schema.id.serialization.enabled", false),
    SCHEMA_VERSION_TRUNCATION_ENABLED("schema.version.truncation.enabled", false),

    GOOGLE_PUBSUB_SENDER_CORE_POOL_SIZE("googlepubsub.sender.core.pool.size", 4),
    GOOGLE_PUBSUB_SENDER_TOTAL_TIMEOUT("googlepubsub.sender.total.timeout.ms", 600_000L),
    GOOGLE_PUBSUB_SENDER_REQUEST_BYTES_THRESHOLD("googlepubsub.sender.batching.request.bytes.threshold", 1024L),
    GOOGLE_PUBSUB_SENDER_MESSAGE_COUNT_BATCH_SIZE("googlepubsub.sender.batching.message.count.bytes.size", 1L),
    GOOGLE_PUBSUB_SENDER_PUBLISH_DELAY_THRESHOLD("googlepubsub.sender.batching.publish.delay.threshold.ms", 1L),
    GOOGLE_PUBSUB_TRANSPORT_CHANNEL_PROVIDER_ADDRESS("googlepubsub.sender.transport.channel.provider.address", "integration"),

    UNDELIVERED_MESSAGE_LOG_PERSIST_PERIOD_MS("undelivered.message.log.persist.period.ms", 5000);

    private final String name;

    private final Object defaultValue;

    Configs(String name, Object defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
    }

    public static Configs getForName(String name) {
        return Arrays.stream(Configs.values())
                .filter(configs -> configs.name.equals(name))
                .reduce((a, b) -> { throw new DuplicateConfigPropertyException(name); })
                .orElseThrow(() -> new MissingConfigPropertyException(name));
    }

    public String getName() {
        return name;
    }

    @SuppressWarnings("unchecked")
    public <T> T getDefaultValue() {
        return (T) defaultValue;
    }
}
