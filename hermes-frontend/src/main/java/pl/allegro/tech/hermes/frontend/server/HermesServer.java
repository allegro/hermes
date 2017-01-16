package pl.allegro.tech.hermes.frontend.server;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.RequestDumpingHandler;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.frontend.publishing.handlers.ThroughputLimiter;
import pl.allegro.tech.hermes.frontend.publishing.preview.MessagePreviewPersister;
import pl.allegro.tech.hermes.frontend.services.HealthCheckService;

import javax.inject.Inject;

import static io.undertow.UndertowOptions.*;
import static org.xnio.Options.BACKLOG;
import static org.xnio.Options.READ_TIMEOUT;
import static pl.allegro.tech.hermes.common.config.Configs.*;

public class HermesServer {

    private Undertow undertow;
    private HermesShutdownHandler gracefulShutdown;

    private final HermesMetrics hermesMetrics;
    private final ConfigFactory configFactory;
    private final HttpHandler publishingHandler;
    private final HealthCheckService healthCheckService;
    private final MessagePreviewPersister messagePreviewPersister;
    private final int port;
    private final int sslPort;
    private final String host;
    private ThroughputLimiter throughputLimiter;

    @Inject
    public HermesServer(
            ConfigFactory configFactory,
            HermesMetrics hermesMetrics,
            HttpHandler publishingHandler,
            HealthCheckService healthCheckService,
            MessagePreviewPersister messagePreviewPersister,
            ThroughputLimiter throughputLimiter) {

        this.configFactory = configFactory;
        this.hermesMetrics = hermesMetrics;
        this.publishingHandler = publishingHandler;
        this.healthCheckService = healthCheckService;
        this.messagePreviewPersister = messagePreviewPersister;

        this.port = configFactory.getIntProperty(FRONTEND_PORT);
        this.sslPort = configFactory.getIntProperty(FRONTEND_SSL_PORT);
        this.host = configFactory.getStringProperty(FRONTEND_HOST);
        this.throughputLimiter = throughputLimiter;
    }

    public void start() {
        configureServer().start();
        messagePreviewPersister.start();
        throughputLimiter.start();

    }

    public void gracefulShutdown() throws InterruptedException {
        healthCheckService.shutdown();

        Thread.sleep(configFactory.getIntProperty(Configs.FRONTEND_GRACEFUL_SHUTDOWN_INITIAL_WAIT_MS));

        gracefulShutdown.handleShutdown();
    }

    public void shutdown() throws InterruptedException {
        undertow.stop();
        messagePreviewPersister.shutdown();
        throughputLimiter.stop();
    }

    private Undertow configureServer() {
        gracefulShutdown = new HermesShutdownHandler(handlers(), hermesMetrics);
        Undertow.Builder builder = Undertow.builder()
                .addHttpListener(port, host)
                .setServerOption(REQUEST_PARSE_TIMEOUT, configFactory.getIntProperty(FRONTEND_REQUEST_PARSE_TIMEOUT))
                .setServerOption(MAX_HEADERS, configFactory.getIntProperty(FRONTEND_MAX_HEADERS))
                .setServerOption(MAX_PARAMETERS, configFactory.getIntProperty(FRONTEND_MAX_PARAMETERS))
                .setServerOption(MAX_COOKIES, configFactory.getIntProperty(FRONTEND_MAX_COOKIES))
                .setServerOption(ALWAYS_SET_KEEP_ALIVE, configFactory.getBooleanProperty(FRONTEND_SET_KEEP_ALIVE))
                .setSocketOption(BACKLOG, configFactory.getIntProperty(FRONTEND_BACKLOG_SIZE))
                .setSocketOption(READ_TIMEOUT, configFactory.getIntProperty(FRONTEND_READ_TIMEOUT))
                .setIoThreads(configFactory.getIntProperty(FRONTEND_IO_THREADS_COUNT))
                .setWorkerThreads(configFactory.getIntProperty(FRONTEND_WORKER_THREADS_COUNT))
                .setBufferSize(configFactory.getIntProperty(FRONTEND_BUFFER_SIZE))
                .setHandler(gracefulShutdown);

        if (configFactory.getBooleanProperty(FRONTEND_SSL_ENABLED)) {
            builder.addHttpsListener(sslPort, host, new SSLContextSupplier(configFactory).get())
                    .setServerOption(ENABLE_HTTP2, configFactory.getBooleanProperty(FRONTEND_HTTP2_ENABLED));
        }
        this.undertow = builder.build();
        return undertow;
    }

    private HttpHandler handlers() {
        HttpHandler healthCheckHandler = new HealthCheckHandler(healthCheckService);

        RoutingHandler routingHandler =  new RoutingHandler()
                .post("/topics/{qualifiedTopicName}", publishingHandler)
                .get("/status/ping", healthCheckHandler)
                .get("/status/health", healthCheckHandler)
                .get("/", healthCheckHandler);

        return isEnabled(FRONTEND_REQUEST_DUMPER) ? new RequestDumpingHandler(routingHandler) : routingHandler;
    }

    private boolean isEnabled(Configs property) {
        return configFactory.getBooleanProperty(property);
    }
}
