package pl.allegro.tech.hermes.common.config;

import com.google.common.io.Files;
import pl.allegro.tech.hermes.common.util.InetAddressInstanceIdResolver;

import java.util.Arrays;

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

    // frontend properties
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
