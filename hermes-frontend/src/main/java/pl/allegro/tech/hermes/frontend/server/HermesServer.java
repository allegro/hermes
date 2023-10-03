package pl.allegro.tech.hermes.frontend.server;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.RequestDumpingHandler;
import org.xnio.SslClientAuthMode;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.frontend.publishing.handlers.ThroughputLimiter;
import pl.allegro.tech.hermes.frontend.publishing.preview.MessagePreviewPersister;
import pl.allegro.tech.hermes.frontend.services.HealthCheckService;

import static io.undertow.UndertowOptions.ALWAYS_SET_KEEP_ALIVE;
import static io.undertow.UndertowOptions.ENABLE_HTTP2;
import static io.undertow.UndertowOptions.MAX_COOKIES;
import static io.undertow.UndertowOptions.MAX_HEADERS;
import static io.undertow.UndertowOptions.MAX_PARAMETERS;
import static io.undertow.UndertowOptions.REQUEST_PARSE_TIMEOUT;
import static org.xnio.Options.BACKLOG;
import static org.xnio.Options.KEEP_ALIVE;
import static org.xnio.Options.READ_TIMEOUT;
import static org.xnio.Options.SSL_CLIENT_AUTH_MODE;

public class HermesServer {

    private final MetricsFacade metricsFacade;
    private final HermesServerParameters hermesServerParameters;
    private final SslParameters sslParameters;
    private final HttpHandler publishingHandler;
    private final HealthCheckService healthCheckService;
    private final ReadinessChecker readinessChecker;
    private final MessagePreviewPersister messagePreviewPersister;
    private final ThroughputLimiter throughputLimiter;
    private final TopicMetadataLoadingJob topicMetadataLoadingJob;
    private final boolean topicMetadataLoadingJobEnabled;
    private final SslContextFactoryProvider sslContextFactoryProvider;
    private final PrometheusMeterRegistry prometheusMeterRegistry;
    private Undertow undertow;
    private HermesShutdownHandler gracefulShutdown;

    public HermesServer(
            SslParameters sslParameters,
            HermesServerParameters hermesServerParameters,
            MetricsFacade metricsFacade,
            HttpHandler publishingHandler,
            ReadinessChecker readinessChecker,
            MessagePreviewPersister messagePreviewPersister,
            ThroughputLimiter throughputLimiter,
            TopicMetadataLoadingJob topicMetadataLoadingJob,
            boolean topicMetadataLoadingJobEnabled,
            SslContextFactoryProvider sslContextFactoryProvider,
            PrometheusMeterRegistry prometheusMeterRegistry) {

        this.sslParameters = sslParameters;
        this.hermesServerParameters = hermesServerParameters;
        this.metricsFacade = metricsFacade;
        this.publishingHandler = publishingHandler;
        this.prometheusMeterRegistry = prometheusMeterRegistry;
        this.healthCheckService = new HealthCheckService();
        this.readinessChecker = readinessChecker;
        this.messagePreviewPersister = messagePreviewPersister;
        this.topicMetadataLoadingJob = topicMetadataLoadingJob;
        this.topicMetadataLoadingJobEnabled = topicMetadataLoadingJobEnabled;
        this.sslContextFactoryProvider = sslContextFactoryProvider;
        this.throughputLimiter = throughputLimiter;
    }

    public void start() {
        configureServer().start();
        messagePreviewPersister.start();
        throughputLimiter.start();

        if (topicMetadataLoadingJobEnabled) {
            topicMetadataLoadingJob.start();
        }
        healthCheckService.startup();
        readinessChecker.start();
    }

    public void stop() throws InterruptedException {
        if (hermesServerParameters.isGracefulShutdownEnabled()) {
            prepareForGracefulShutdown();
        }
        shutdown();
    }

    public void prepareForGracefulShutdown() throws InterruptedException {
        healthCheckService.shutdown();

        Thread.sleep(hermesServerParameters.getGracefulShutdownInitialWait().toMillis());

        gracefulShutdown.handleShutdown();
    }

    public void shutdown() throws InterruptedException {
        undertow.stop();
        messagePreviewPersister.shutdown();
        throughputLimiter.stop();

        if (topicMetadataLoadingJobEnabled) {
            topicMetadataLoadingJob.stop();
        }
        readinessChecker.stop();
    }

    private Undertow configureServer() {
        gracefulShutdown = new HermesShutdownHandler(handlers(), metricsFacade);
        Undertow.Builder builder = Undertow.builder()
                .addHttpListener(hermesServerParameters.getPort(), hermesServerParameters.getHost())
                .setServerOption(REQUEST_PARSE_TIMEOUT, (int) hermesServerParameters.getRequestParseTimeout().toMillis())
                .setServerOption(MAX_HEADERS, hermesServerParameters.getMaxHeaders())
                .setServerOption(MAX_PARAMETERS, hermesServerParameters.getMaxParameters())
                .setServerOption(MAX_COOKIES, hermesServerParameters.getMaxCookies())
                .setServerOption(ALWAYS_SET_KEEP_ALIVE, hermesServerParameters.isAlwaysKeepAlive())
                .setServerOption(KEEP_ALIVE, hermesServerParameters.isKeepAlive())
                .setSocketOption(BACKLOG, hermesServerParameters.getBacklogSize())
                .setSocketOption(READ_TIMEOUT, (int) hermesServerParameters.getReadTimeout().toMillis())
                .setIoThreads(hermesServerParameters.getIoThreadsCount())
                .setWorkerThreads(hermesServerParameters.getWorkerThreadCount())
                .setBufferSize(hermesServerParameters.getBufferSize())
                .setHandler(gracefulShutdown);

        if (sslParameters.isEnabled()) {
            builder.addHttpsListener(sslParameters.getPort(), hermesServerParameters.getHost(),
                            sslContextFactoryProvider.getSslContextFactory().create().getSslContext())
                    .setSocketOption(SSL_CLIENT_AUTH_MODE,
                            SslClientAuthMode.valueOf(sslParameters.getClientAuthMode().toUpperCase()))
                    .setServerOption(ENABLE_HTTP2, hermesServerParameters.isHttp2Enabled());
        }
        this.undertow = builder.build();
        return undertow;
    }

    private HttpHandler handlers() {
        HttpHandler healthCheckHandler = new HealthCheckHandler(healthCheckService);
        HttpHandler readinessHandler = new ReadinessCheckHandler(readinessChecker, healthCheckService);
        HttpHandler prometheusHandler = new PrometheusMetricsHandler(prometheusMeterRegistry);

        RoutingHandler routingHandler = new RoutingHandler()
                .post("/topics/{qualifiedTopicName}", publishingHandler)
                .get("/status/ping", healthCheckHandler)
                .get("/status/health", healthCheckHandler)
                .get("/status/ready", readinessHandler)
                .get("/status/prometheus", prometheusHandler)
                .get("/", healthCheckHandler);

        return isFrontendRequestDumperEnabled() ? new RequestDumpingHandler(routingHandler) : routingHandler;
    }

    private boolean isFrontendRequestDumperEnabled() {
        return hermesServerParameters.isRequestDumperEnabled();
    }
}
