package pl.allegro.tech.hermes.frontend.server;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.RequestDumpingHandler;
import org.xnio.SslClientAuthMode;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
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

    private Undertow undertow;
    private HermesShutdownHandler gracefulShutdown;

    private final HermesMetrics hermesMetrics;
    private final HttpHandler publishingHandler;
    private final HermesServerParameters serverParameters;
    private final HealthCheckService healthCheckService;
    private final ReadinessChecker readinessChecker;
    private final MessagePreviewPersister messagePreviewPersister;
    private final int port;
    private final int sslPort;
    private final String host;
    private final ThroughputLimiter throughputLimiter;
    private final TopicMetadataLoadingJob topicMetadataLoadingJob;
    private final SslContextFactoryProvider sslContextFactoryProvider;

    public HermesServer(
            HermesServerParameters serverParameters,
            HermesMetrics hermesMetrics,
            HttpHandler publishingHandler,
            ReadinessChecker readinessChecker,
            MessagePreviewPersister messagePreviewPersister,
            ThroughputLimiter throughputLimiter,
            TopicMetadataLoadingJob topicMetadataLoadingJob,
            SslContextFactoryProvider sslContextFactoryProvider) {

        this.serverParameters = serverParameters;
        this.hermesMetrics = hermesMetrics;
        this.publishingHandler = publishingHandler;
        this.healthCheckService = new HealthCheckService();
        this.readinessChecker = readinessChecker;
        this.messagePreviewPersister = messagePreviewPersister;
        this.topicMetadataLoadingJob = topicMetadataLoadingJob;
        this.sslContextFactoryProvider = sslContextFactoryProvider;

        this.port = serverParameters.getFrontendPort();
        this.sslPort = serverParameters.getSslPort();
        this.host = serverParameters.getFrontHost();
        this.throughputLimiter = throughputLimiter;
    }

    public void start() {
        configureServer().start();
        messagePreviewPersister.start();
        throughputLimiter.start();

        if (serverParameters.isTopicMetadataRefreshJobEnabled()) {
            topicMetadataLoadingJob.start();
        }
        healthCheckService.startup();
        readinessChecker.start();
    }

    public void stop() throws InterruptedException {
        if(serverParameters.isGracefulShutdownEnabled()) {
            prepareForGracefulShutdown();
        }
        shutdown();
    }

    public void prepareForGracefulShutdown() throws InterruptedException {
        healthCheckService.shutdown();

        Thread.sleep(serverParameters.getGracefulShutdownInitialWaitMs());

        gracefulShutdown.handleShutdown();
    }

    public void shutdown() throws InterruptedException {
        undertow.stop();
        messagePreviewPersister.shutdown();
        throughputLimiter.stop();

        if (serverParameters.isTopicMetadataRefreshJobEnabled()) {
            topicMetadataLoadingJob.stop();
        }
        readinessChecker.stop();
    }

    private Undertow configureServer() {
        gracefulShutdown = new HermesShutdownHandler(handlers(), hermesMetrics);
        Undertow.Builder builder = Undertow.builder()
                .addHttpListener(port, host)
                .setServerOption(REQUEST_PARSE_TIMEOUT, serverParameters.getRequestParseTimeout())
                .setServerOption(MAX_HEADERS, serverParameters.getMaxHeaders())
                .setServerOption(MAX_PARAMETERS, serverParameters.getMaxParameters())
                .setServerOption(MAX_COOKIES, serverParameters.getMaxCookies())
                .setServerOption(ALWAYS_SET_KEEP_ALIVE, serverParameters.isAlwaysSetKepAlive())
                .setServerOption(KEEP_ALIVE, serverParameters.isSetKeepAlive())
                .setSocketOption(BACKLOG, serverParameters.getBacklogSize())
                .setSocketOption(READ_TIMEOUT, serverParameters.getReadTimeout())
                .setIoThreads(serverParameters.getIoThreadCount())
                .setWorkerThreads(serverParameters.getWorkerThreadCount())
                .setBufferSize(serverParameters.getBufferSize())
                .setHandler(gracefulShutdown);

        if (serverParameters.isSslEnabled()) {
            builder.addHttpsListener(sslPort, host, sslContextFactoryProvider.getSslContextFactory().create().getSslContext())
                    .setSocketOption(SSL_CLIENT_AUTH_MODE,
                            SslClientAuthMode.valueOf(serverParameters.getSslClientAuthMode().toUpperCase()))
                    .setServerOption(ENABLE_HTTP2, serverParameters.isHttp2Enabled());
        }
        this.undertow = builder.build();
        return undertow;
    }

    private HttpHandler handlers() {
        HttpHandler healthCheckHandler = new HealthCheckHandler(healthCheckService);
        HttpHandler readinessHandler = new ReadinessCheckHandler(readinessChecker, healthCheckService);

        RoutingHandler routingHandler =  new RoutingHandler()
                .post("/topics/{qualifiedTopicName}", publishingHandler)
                .get("/status/ping", healthCheckHandler)
                .get("/status/health", healthCheckHandler)
                .get("/status/ready", readinessHandler)
                .get("/", healthCheckHandler);

        return serverParameters.isRequestDumper() ? new RequestDumpingHandler(routingHandler) : routingHandler;
    }
}
