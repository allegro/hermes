package pl.allegro.tech.hermes.integration.env;

import com.codahale.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.consumers.HermesConsumers;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.EndpointAddressResolver;
import pl.allegro.tech.hermes.metrics.PathsCompiler;
import pl.allegro.tech.hermes.test.helper.config.MutableConfigFactory;
import pl.allegro.tech.hermes.test.helper.endpoint.MultiUrlEndpointAddressResolver;
import pl.allegro.tech.hermes.test.helper.environment.Starter;
import pl.allegro.tech.hermes.tracker.mongo.consumers.MongoLogRepository;

import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_USE_TOPIC_MESSAGE_SIZE;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_CONSUMER_AUTO_OFFSET_RESET_CONFIG;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_CONSUMER_HEARTBEAT_INTERVAL_MS_CONFIG;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_CONSUMER_MAX_POLL_RECORDS_CONFIG;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_CONSUMER_RECONNECT_BACKOFF_MS_CONFIG;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_CONSUMER_REQUEST_TIMEOUT_MS_CONFIG;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_CONSUMER_RETRY_BACKOFF_MS_CONFIG;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_CONSUMER_SESSION_TIMEOUT_MS_CONFIG;
import static pl.allegro.tech.hermes.common.config.Configs.SCHEMA_CACHE_ENABLED;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_SSL_KEYSTORE_SOURCE;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_SSL_TRUSTSTORE_SOURCE;

public class ConsumersStarter implements Starter<HermesConsumers> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumersStarter.class);

    private final MutableConfigFactory configFactory = new MutableConfigFactory();
    private HermesConsumers consumers;

    @Override
    public void start() throws Exception {
        LOGGER.info("Starting Hermes Consumers");
        configFactory.overrideProperty(SCHEMA_CACHE_ENABLED, true);
        configFactory.overrideProperty(KAFKA_CONSUMER_AUTO_OFFSET_RESET_CONFIG, "earliest");
        configFactory.overrideProperty(KAFKA_CONSUMER_RECONNECT_BACKOFF_MS_CONFIG, 25);
        configFactory.overrideProperty(KAFKA_CONSUMER_RETRY_BACKOFF_MS_CONFIG, 25);
        configFactory.overrideProperty(KAFKA_CONSUMER_MAX_POLL_RECORDS_CONFIG, 1);
        configFactory.overrideProperty(KAFKA_CONSUMER_REQUEST_TIMEOUT_MS_CONFIG, 11000);
        configFactory.overrideProperty(KAFKA_CONSUMER_SESSION_TIMEOUT_MS_CONFIG, 10000);
        configFactory.overrideProperty(KAFKA_CONSUMER_HEARTBEAT_INTERVAL_MS_CONFIG, 50);
        configFactory.overrideProperty(CONSUMER_USE_TOPIC_MESSAGE_SIZE, true);
        configFactory.overrideProperty(CONSUMER_SSL_KEYSTORE_SOURCE, "provided");
        configFactory.overrideProperty(CONSUMER_SSL_TRUSTSTORE_SOURCE, "provided");

        consumers = HermesConsumers.consumers()
//                .withKafkaTopicsNamesMapper(
//                        new IntegrationTestKafkaNamesMapperFactory(configFactory.getStringProperty(Configs.KAFKA_NAMESPACE)).create())
                .withSpringKafkaTopicsNamesMapper(
                        () -> new IntegrationTestKafkaNamesMapperFactory(configFactory.getStringProperty(Configs.KAFKA_NAMESPACE)).create()
                )
//                .withBinding(configFactory, ConfigFactory.class)
                .withSpringBinding(() -> configFactory, ConfigFactory.class)
//                .withBinding(new MultiUrlEndpointAddressResolver(), EndpointAddressResolver.class)
                .withSpringBinding(MultiUrlEndpointAddressResolver::new, EndpointAddressResolver.class)
//                .withLogRepository(serviceLocator -> new MongoLogRepository(FongoFactory.hermesDB(),
//                        10,
//                        1000,
//                        configFactory.getStringProperty(Configs.KAFKA_CLUSTER_NAME),
//                        configFactory.getStringProperty(Configs.HOSTNAME),
//                        serviceLocator.getService(MetricRegistry.class),
//                        serviceLocator.getService(PathsCompiler.class)))
                .withSpringLogRepository(applicationContext -> new MongoLogRepository(FongoFactory.hermesDB(),
                        10,
                        1000,
                        configFactory.getStringProperty(Configs.KAFKA_CLUSTER_NAME),
                        configFactory.getStringProperty(Configs.HOSTNAME),
                        applicationContext.getBean(MetricRegistry.class),
                        applicationContext.getBean(PathsCompiler.class)))
//                .withDisabledGlobalShutdownHook()
                .withSpringDisabledGlobalShutdownHook()
                .withDisabledFlushLogsShutdownHook()
                .build();

        consumers.start();
    }

    @Override
    public HermesConsumers instance() {
        return consumers;
    }

    @Override
    public void stop() throws Exception {
        LOGGER.info("Stopping Hermes Consumers");
        consumers.stop();
    }

    public void overrideProperty(Configs config, Object value) {
        configFactory.overrideProperty(config, value);
    }

}
