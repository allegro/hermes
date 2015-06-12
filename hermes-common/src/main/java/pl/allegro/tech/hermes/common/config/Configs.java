package pl.allegro.tech.hermes.common.config;

@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
public enum Configs {

    ZOOKEEPER_CONNECT_STRING("zookeeper.connect.string", "localhost:2181"),
    ZOOKEEPER_BASE_SLEEP_TIME("zookeeper.base.sleep.time", 1000),
    ZOOKEEPER_CONNECTION_TIMEOUT("zookeeper.connection.timeout", 10000),
    ZOOKEEPER_SESSION_TIMEOUT("zookeeper.session.timeout", 10000),
    ZOOKEEPER_SYNC_TIME("zookeeper.sync.time", 20000),

    ZOOKEEPER_MAX_RETRIES("zookeeper.max.retries", 2),
    ZOOKEEPER_ROOT("zookeeper.root", "/hermes"),
    ZOOKEEPER_CACHE_THREAD_POOL_SIZE("zookeeper.cache.thread.pool.size", 5),

    KAFKA_ZOOKEEPER_CONNECT_STRING("kafka.zookeeper.connect.string", "localhost:2181"),

    ENVIRONMENT_NAME("environment.name", "dev"),

    KAFKA_CLUSTER_NAME("kafka.cluster.name", "primary"),
    KAFKA_BROKER_LIST("kafka.broker.list", "localhost:9092"),
    KAFKA_CONSUMER_TIMEOUT_MS("kafka.consumer.timeout.ms", 60000),
    KAFKA_CONSUMER_AUTO_OFFSET_RESET("kafka.consumer.auto.offset.reset", "largest"),
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
    FRONTEND_HTTP2("frontend.http2", true),

    FRONTEND_SSL_PORT("frontend.ssl.port", 8081),
    FRONTEND_SSL_PROTOCOL("frontend.ssl.protocol", "TLS"),
    FRONTEND_SSL_KEYSTORE_LOCATION("frontend.ssl.keystore.location", "server.keystore"),
    FRONTEND_SSL_KEYSTORE_PASSWORD("frontend.ssl.keystore.password", "password"),
    FRONTEND_SSL_KEYSTORE_FORMAT("frontend.ssl.keystore.format", "JKS"),
    FRONTEND_SSL_TRUSTSTORE_LOCATION("frontend.ssl.truststore.location", "server.truststore"),
    FRONTEND_SSL_TRUSTSTORE_PASSWORD("frontend.ssl.truststore.password", "password"),
    FRONTEND_SSL_TRUSTSTORE_FORMAT("frontend.ssl.truststore.format", "JKS"),


    CONSUMER_COMMIT_OFFSET_PERIOD("consumer.commit.offset.period", 20),
    CONSUMER_COMMIT_OFFSET_CACHE_EXPIRATION("consumer.commit.offset.cache.expiration", 10),
    CONSUMER_SENDER_ASYNC_TIMEOUT_MS("consumer.sender.async.timeout.ms", 5_000),
    CONSUMER_SENDER_ASYNC_TIMEOUT_THREAD_POOL_SIZE("consumer.sender.async.timeout.thread.pool.size", 32),
    CONSUMER_THREAD_POOL_SIZE("consumer.thread.pool.size", 500),
    CONSUMER_HTTP_CLIENT_REQUEST_TIMEOUT("consumer.http.client.request.timeout", 1000),
    CONSUMER_HTTP_CLIENT_THREAD_POOL_SIZE("consumer.http.client.thread.pool.size", 30),
    CONSUMER_HTTP_CLIENT_MAX_CONNECTIONS_PER_DESTINATION("consumer.http.client.max.connections.per.destination", 100),
    CONSUMER_HTTP_CLIENT_CONNECTIONS_QUEUE("consumer.http.client.connections.per.queue", 1000000),

    CONSUMER_INFLIGHT_SIZE("consumer.inflight.size", 100),
    CONSUMER_RATE_LIMITER_SUPERVISOR_PERIOD("consumer.rate.limiter.supervisor.period", 30),
    CONSUMER_RATE_LIMITER_REPORTING_THREAD_POOL_SIZE("consumer.rate.limiter.reporting.thread.pool.size", 30),
    CONSUMER_RATE_LIMITER_HEARTBEAT_MODE_DELAY("consumer.rate.limiter.hearbeat.mode.delay", 60),
    CONSUMER_RATE_LIMITER_SLOW_MODE_DELAY("consumer.rate.limiter.slow.mode.delay", 1),
    CONSUMER_RATE_CONVERGENCE_FACTOR("consumer.rate.convergence.factor", 0.2),
    CONSUMER_RATE_FAILURES_RATIO_THRESHOLD("consumer.rate.failures.ratio.threshold", 0.01),
    CONSUMER_OFFSET_MONITOR_ENABLED("consumer.offset.monitor.enabled", true),
    CONSUMER_OFFSET_COMMIT_QUEUE_ALERT_MINIMAL_IDLE_PERIOD("consumer.offset.commit.queue.alert.minimal.idle.period", 3600),
    CONSUMER_OFFSET_COMMIT_QUEUE_ALERT_SIZE("consumer.offset.commit.queue.alert.size", 20_000),
    CONSUMER_HEALTH_CHECK_PORT("consumer.status.health.port", 8000),

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
    METADATA_CONTENT_ROOT("message.content.root", "metadata"),
    GRAPHITE_PREFIX("graphite.prefix", "stats.tech.hermes"),

    TRACKER_MONGODB_URI("tracker.mongodb.uri", "mongodb://localhost:27017/hermesMessages"),
    TRACKER_MONGODB_COMMIT_INTERVAL("tracker.mongodb.commit.interval", 1000),
    TRACKER_MONGODB_QUEUE_CAPACITY("tracker.mongodb.queue.capacity", 100 * 1000),
    UNDELIVERED_MESSAGE_LOG_PERSIST_PERIOD_MS("undelivered.message.log.persist.period.ms", 5000);

    private final String name;

    private final Object defaultValue;

    private Configs(String name, Object defaultValue) {
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
