package pl.allegro.tech.hermes.frontend;

import com.google.common.collect.Lists;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.di.CommonBinder;
import pl.allegro.tech.hermes.common.hook.FlushLogsShutdownHook;
import pl.allegro.tech.hermes.common.hook.Hook;
import pl.allegro.tech.hermes.common.hook.HooksHandler;
import pl.allegro.tech.hermes.common.hook.ServiceAwareHook;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.frontend.di.FrontendBinder;
import pl.allegro.tech.hermes.frontend.di.LoggerConfiguration;
import pl.allegro.tech.hermes.frontend.di.PersistentBufferExtension;
import pl.allegro.tech.hermes.frontend.di.TrackersBinder;
import pl.allegro.tech.hermes.frontend.listeners.BrokerAcknowledgeListener;
import pl.allegro.tech.hermes.frontend.listeners.BrokerErrorListener;
import pl.allegro.tech.hermes.frontend.listeners.BrokerListeners;
import pl.allegro.tech.hermes.frontend.listeners.BrokerTimeoutListener;
import pl.allegro.tech.hermes.frontend.publishing.metadata.HeadersPropagator;
import pl.allegro.tech.hermes.frontend.server.AbstractShutdownHook;
import pl.allegro.tech.hermes.frontend.server.HermesServer;
import pl.allegro.tech.hermes.common.ssl.SslContextFactory;
import pl.allegro.tech.hermes.frontend.server.TopicMetadataLoadingStartupHook;
import pl.allegro.tech.hermes.frontend.server.TopicSchemaLoadingStartupHook;
import pl.allegro.tech.hermes.frontend.server.WaitForKafkaStartupHook;
import pl.allegro.tech.hermes.frontend.server.auth.AuthenticationConfiguration;
import pl.allegro.tech.hermes.frontend.services.HealthCheckService;
import pl.allegro.tech.hermes.infrastructure.zookeeper.cache.ModelAwareZookeeperNotifyingCache;
import pl.allegro.tech.hermes.tracker.frontend.LogRepository;
import pl.allegro.tech.hermes.tracker.frontend.Trackers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static pl.allegro.tech.hermes.common.config.Configs.FRONTEND_GRACEFUL_SHUTDOWN_ENABLED;
import static pl.allegro.tech.hermes.common.config.Configs.FRONTEND_RESPONSE_ERROR_LOGGER_ENABLED;
import static pl.allegro.tech.hermes.common.config.Configs.FRONTEND_STARTUP_TOPIC_METADATA_LOADING_ENABLED;
import static pl.allegro.tech.hermes.common.config.Configs.FRONTEND_STARTUP_TOPIC_SCHEMA_LOADING_ENABLED;
import static pl.allegro.tech.hermes.common.config.Configs.FRONTEND_STARTUP_WAIT_KAFKA_ENABLED;

public final class HermesFrontend {

    private static final Logger logger = LoggerFactory.getLogger(HermesFrontend.class);

    private final ServiceLocator serviceLocator;
    private final HooksHandler hooksHandler;
    private final List<Function<ServiceLocator, LogRepository>> logRepositories;
    private final HermesServer hermesServer;
    private final Trackers trackers;

    public static void main(String[] args) throws Exception {
        frontend().build().start();
    }

    private HermesFrontend(HooksHandler hooksHandler,
                           List<Binder> binders,
                           List<Function<ServiceLocator, LogRepository>> logRepositories,
                           boolean flushLogsShutdownHookEnabled) {
        this.hooksHandler = hooksHandler;
        this.logRepositories = logRepositories;

        serviceLocator = createDIContainer(binders);

        hermesServer = serviceLocator.getService(HermesServer.class);
        trackers = serviceLocator.getService(Trackers.class);

        ConfigFactory config = serviceLocator.getService(ConfigFactory.class);
        if (config.getBooleanProperty(FRONTEND_GRACEFUL_SHUTDOWN_ENABLED)) {
            hooksHandler.addShutdownHook(gracefulShutdownHook());
        }
        if (config.getBooleanProperty(FRONTEND_STARTUP_WAIT_KAFKA_ENABLED)) {
            hooksHandler.addBeforeStartHook(serviceLocator.getService(WaitForKafkaStartupHook.class));
        }
        if (config.getBooleanProperty(FRONTEND_STARTUP_TOPIC_METADATA_LOADING_ENABLED)) {
            hooksHandler.addBeforeStartHook(serviceLocator.getService(TopicMetadataLoadingStartupHook.class));
        }
        if (config.getBooleanProperty(FRONTEND_STARTUP_TOPIC_SCHEMA_LOADING_ENABLED)) {
            hooksHandler.addBeforeStartHook(serviceLocator.getService(TopicSchemaLoadingStartupHook.class));
        }
        hooksHandler.addStartupHook((s) -> s.getService(HealthCheckService.class).startup());
        hooksHandler.addShutdownHook(defaultShutdownHook());
        if (flushLogsShutdownHookEnabled) {
            hooksHandler.addShutdownHook(new FlushLogsShutdownHook());
        }
        if (!config.getBooleanProperty(FRONTEND_RESPONSE_ERROR_LOGGER_ENABLED)) {
            LoggerConfiguration.disableResponseErrorLogger();
        }
    }

    private ServiceAwareHook gracefulShutdownHook() {
        return new AbstractShutdownHook() {
            @Override
            public void shutdown() throws InterruptedException {
                hermesServer.gracefulShutdown();
            }

            @Override
            public int getPriority() {
                return Hook.HIGHER_PRIORITY;
            }
        };
    }

    private AbstractShutdownHook defaultShutdownHook() {
        return new AbstractShutdownHook() {
            @Override
            public void shutdown() throws InterruptedException {
                hermesServer.shutdown();
                serviceLocator.shutdown();
            }
        };
    }

    public void start() {
        logRepositories.forEach(serviceLocatorLogRepositoryFunction ->
                trackers.add(serviceLocatorLogRepositoryFunction.apply(serviceLocator)));

        serviceLocator.getService(PersistentBufferExtension.class).extend();
        startCaches(serviceLocator);

        hooksHandler.runBeforeStartHooks(serviceLocator);
        hermesServer.start();
        hooksHandler.startup(serviceLocator);
    }

    private void startCaches(ServiceLocator locator) {
        try {
            locator.getService(ModelAwareZookeeperNotifyingCache.class).start();
        } catch(Exception e) {
            logger.error("Failed to startup Hermes Frontend", e);
        }
    }

    public void stop() {
        hooksHandler.shutdown(serviceLocator);
    }

    public <T> T getService(Class<T> clazz) {
        return serviceLocator.getService(clazz);
    }

    public <T> T getService(Class<T> clazz, String name) {
        return serviceLocator.getService(clazz, name);
    }

    private ServiceLocator createDIContainer(List<Binder> binders) {
        String uniqueName = "HermesFrontendLocator" + UUID.randomUUID();

        return ServiceLocatorUtilities.bind(uniqueName, binders.toArray(new Binder[binders.size()]));
    }

    public static Builder frontend() {
        return new Builder();
    }

    public static final class Builder {

        private static final int CUSTOM_BINDER_HIGH_PRIORITY = 10;

        private final HooksHandler hooksHandler = new HooksHandler();
        private final List<Binder> binders = Lists.newArrayList(
                new CommonBinder(),
                new FrontendBinder(hooksHandler)
        );
        private final BrokerListeners listeners = new BrokerListeners();
        private final List<Function<ServiceLocator, LogRepository>> logRepositories = new ArrayList<>();
        private boolean flushLogsShutdownHookEnabled = true;

        public HermesFrontend build() {
            withDefaultRankBinding(listeners, BrokerListeners.class);
            binders.add(new TrackersBinder(new ArrayList<>()));
            return new HermesFrontend(hooksHandler, binders, logRepositories, flushLogsShutdownHookEnabled);
        }

        public Builder withBeforeStartHook(ServiceAwareHook hook) {
            hooksHandler.addBeforeStartHook(hook);
            return this;
        }

        public Builder withBeforeStartHook(Hook hook) {
            withBeforeStartHook(s -> hook.apply());
            return this;
        }

        public Builder withStartupHook(ServiceAwareHook hook) {
            hooksHandler.addStartupHook(hook);
            return this;
        }

        public Builder withStartupHook(Hook hook) {
            return withStartupHook(s -> hook.apply());
        }

        public Builder withShutdownHook(ServiceAwareHook hook) {
            hooksHandler.addShutdownHook(hook);
            return this;
        }

        public Builder withShutdownHook(Hook hook) {
            return withShutdownHook(s -> hook.apply());
        }


        public Builder withDisabledGlobalShutdownHook() {
            hooksHandler.disableGlobalShutdownHook();
            return this;
        }

        public Builder withDisabledFlushLogsShutdownHook() {
            flushLogsShutdownHookEnabled = false;
            return this;
        }

        public Builder withBrokerTimeoutListener(BrokerTimeoutListener brokerTimeoutListener) {
            listeners.addTimeoutListener(brokerTimeoutListener);
            return this;
        }

        public Builder withBrokerAcknowledgeListener(BrokerAcknowledgeListener brokerAcknowledgeListener) {
            listeners.addAcknowledgeListener(brokerAcknowledgeListener);
            return this;
        }

        public Builder withBrokerErrorListener(BrokerErrorListener brokerErrorListener) {
            listeners.addErrorListener(brokerErrorListener);
            return this;
        }

        public Builder withLogRepository(Function<ServiceLocator, LogRepository> logRepository) {
            logRepositories.add(logRepository);
            return this;
        }

        public Builder withHeadersPropagator(HeadersPropagator headersPropagator) {
            return withBinding(headersPropagator, HeadersPropagator.class);
        }

        public Builder withKafkaTopicsNamesMapper(KafkaNamesMapper kafkaNamesMapper) {
            return withBinding(kafkaNamesMapper, KafkaNamesMapper.class);
        }

        public Builder withAuthenticationConfiguration(AuthenticationConfiguration authenticationConfiguration) {
            return withBinding(authenticationConfiguration, AuthenticationConfiguration.class);
        }

        public Builder withSslContextFactory(SslContextFactory sslContextFactory) {
            return withBinding(sslContextFactory, SslContextFactory.class);
        }

        public <T> Builder withBinding(T instance, Class<T> clazz) {
            return withBinding(instance, clazz, clazz.getName());
        }

        public <T> Builder withBinding(T instance, Class<T> clazz, String name) {
            binders.add(new AbstractBinder() {
                @Override
                protected void configure() {
                    bind(instance).to(clazz).named(name).ranked(CUSTOM_BINDER_HIGH_PRIORITY);
                }
            });
            return this;
        }

        private <T> Builder withDefaultRankBinding(T instance, Class<T> clazz) {
            binders.add(new AbstractBinder() {
                @Override
                protected void configure() {
                    bind(instance).to(clazz).named(clazz.getName());
                }
            });
            return this;
        }
    }
}

