package pl.allegro.tech.hermes.frontend.server;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.RequestDumpingHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.frontend.HermesFrontend;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.publishing.PublishingServlet;
import pl.allegro.tech.hermes.frontend.services.HealthCheckService;

import javax.inject.Inject;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.util.Collections;

import static io.undertow.Handlers.path;
import static io.undertow.Handlers.redirect;
import static io.undertow.UndertowOptions.*;
import static io.undertow.servlet.Servlets.deployment;
import static io.undertow.servlet.Servlets.servlet;
import static org.xnio.Options.BACKLOG;
import static org.xnio.Options.READ_TIMEOUT;
import static pl.allegro.tech.hermes.common.config.Configs.*;

public class HermesServer {

    private Undertow undertow;
    private HermesShutdownHandler gracefulShutdown;

    private final HermesMetrics hermesMetrics;
    private final ConfigFactory configFactory;
    private final TopicsCache topicsCache;
    private final PublishingServlet publishingServlet;
    private final HealthCheckService healthCheckService;
    private final int port;
    private final int sslPort;
    private final String host;

    @Inject
    public HermesServer(
            TopicsCache topicsCache,
            ConfigFactory configFactory,
            HermesMetrics hermesMetrics,
            PublishingServlet publishingServlet,
            HealthCheckService healthCheckService) {

        this.topicsCache = topicsCache;
        this.configFactory = configFactory;
        this.hermesMetrics = hermesMetrics;
        this.publishingServlet = publishingServlet;
        this.healthCheckService = healthCheckService;

        this.port = configFactory.getIntProperty(FRONTEND_PORT);
        this.sslPort = configFactory.getIntProperty(FRONTEND_SSL_PORT);
        this.host = configFactory.getStringProperty(FRONTEND_HOST);
    }

    public void start() {
        topicsCache.start(Collections.emptyList());
        configureServer().start();
    }

    public void gracefulShutdown() throws InterruptedException {
        healthCheckService.shutdown();

        Thread.sleep(configFactory.getIntProperty(Configs.FRONTEND_GRACEFUL_SHUTDOWN_INITIAL_WAIT_MS));

        gracefulShutdown.handleShutdown();
    }

    public void shutdown() throws InterruptedException {
        undertow.stop();
    }

    private Undertow configureServer() {
        gracefulShutdown = new HermesShutdownHandler(deployAndStart(), hermesMetrics);
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

    private PathHandler deployAndStart() {
        try {
            HttpHandler handler = deploy().start();
            handler = isEnabled(FRONTEND_REQUEST_DUMPER) ? new RequestDumpingHandler(handler) : handler;
            return path().addExactPath("/", redirect("/status/health"))
                    .addExactPath("/status/ping", redirect("/status/health"))
                    .addPrefixPath("/status/health", new HealthCheckHandler(healthCheckService))
                    .addPrefixPath("/", handler);
        } catch (ServletException e) {
            throw new IllegalStateException("Something went wrong while starting servlet in undertow", e);
        }
    }

    private DeploymentManager deploy() {
        DeploymentManager manager = Servlets.defaultContainer().addDeployment(prepareDeployment());
        manager.deploy();
        return manager;
    }

    private DeploymentInfo prepareDeployment() {
        HermesDispatcher dispatcher = new HermesDispatcher(publishingServlet, new NotFoundServlet(), "topics");
        ServletInfo dispatcherInfo = servletInfo("dispatcher", "/*", HermesDispatcher.class, dispatcher);
        return deployment()
                .setClassLoader(HermesFrontend.class.getClassLoader())
                .setContextPath("/")
                .setDeploymentName("hermes")
                .addServlet(dispatcherInfo);
    }

    private ServletInfo servletInfo(String name, String mapping, Class<? extends Servlet> clazz, Servlet servlet) {
        return servlet(name, clazz, new ImmediateInstanceFactory<>(servlet))
                .addMapping(mapping).setAsyncSupported(true).setLoadOnStartup(1);
    }

    private boolean isEnabled(Configs property) {
        return configFactory.getBooleanProperty(property);
    }
}
