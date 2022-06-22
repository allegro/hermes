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
    public static String FRONTEND_AUTHENTICATION_MODE = "frontend.authentication.mode";
    public static String FRONTEND_AUTHENTICATION_ENABLED = "frontend.authentication.enabled";
    public static String FRONTEND_KEEP_ALIVE_HEADER_ENABLED = "frontend.keepAliveHeader.enabled";
    public static String FRONTEND_KEEP_ALIVE_HEADER_TIMEOUT_SECONDS = "frontend.keepAliveHeader.timeoutSeconds";
    public static String METRICS_ZOOKEEPER_REPORTER_ENABLED = "frontend.metrics.zookeeperReporterEnabled";
    public static String METRICS_GRAPHITE_REPORTER_ENABLED = "frontend.metrics.graphiteReporterEnabled";
}
