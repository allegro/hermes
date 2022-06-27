package pl.allegro.tech.hermes.frontend;

public class FrontendConfigurationProperties {

    public static String MESSAGES_LOCAL_STORAGE_DIRECTORY = "frontend.messages.local.storage.directory";
    public static String MESSAGES_LOCAL_STORAGE_ENABLED = "frontend.messages.local.storage.enabled";
    public static String FRONTEND_THROUGHPUT_TYPE = "frontend.throughput.type";
    public static String FRONTEND_THROUGHPUT_FIXED_MAX = "frontend.throughput.fixedMax";
    public static String FRONTEND_MESSAGE_PREVIEW_ENABLED = "frontend.message.preview.enabled";
    public static String FRONTEND_MESSAGE_PREVIEW_LOG_PERSIST_PERIOD = "frontend.message.preview.logPersistPeriodSeconds";
    public static String FRONTEND_READINESS_CHECK_ENABLED = "frontend.readiness.check.enabled";
    public static String FRONTEND_READINESS_CHECK_INTERVAL_SECONDS = "frontend.readiness.check.intervalSeconds";
    public static String FRONTEND_AUTHENTICATION_MODE = "frontend.handlers.authentication.mode";
    public static String FRONTEND_AUTHENTICATION_ENABLED = "frontend.handlers.authentication.enabled";
    public static String FRONTEND_KEEP_ALIVE_HEADER_ENABLED = "frontend.handlers.keepAliveHeader.enabled";
    public static String FRONTEND_KEEP_ALIVE_HEADER_TIMEOUT_SECONDS = "frontend.handlers.keepAliveHeader.timeoutSeconds";
    public static String METRICS_ZOOKEEPER_REPORTER_ENABLED = "frontend.metrics.zookeeperReporterEnabled";
    public static String METRICS_GRAPHITE_REPORTER_ENABLED = "frontend.metrics.graphiteReporterEnabled";
    public static String SCHEMA_CACHE_ENABLED = "frontend.schema.cache.enabled";
    public static String SCHEMA_REPOSITORY_SERVER_URL = "frontend.schema.repository.serverUrl";
    public static String FRONTEND_SSL_ENABLED = "frontend.ssl.enabled";
    public static String FRONTEND_SSL_PORT = "frontend.ssl.port";
    public static String FRONTEND_SSL_KEYSTORE_SOURCE = "frontend.ssl.keystoreSource";
    public static String FRONTEND_SSL_TRUSTSTORE_SOURCE = "frontend.ssl.truststoreSource";
    public static String FRONTEND_FORCE_TOPIC_MAX_MESSAGE_SIZE = "frontend.handlers.forceTopicMaxMessageSize";
    public static String FRONTEND_HTTP2_ENABLED = "frontend.server.http2Enabled";
    public static String FRONTEND_GRACEFUL_SHUTDOWN_ENABLED = "frontend.server.gracefulShutdownEnabled";
    public static String FRONTEND_PORT = "frontend.server.port";
}
