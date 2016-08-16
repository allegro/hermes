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

import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_CONSUMER_AUTO_OFFSET_RESET_CONFIG;
import static pl.allegro.tech.hermes.common.config.Configs.SCHEMA_CACHE_ENABLED;
import static pl.allegro.tech.hermes.common.config.Configs.SCHEMA_REPOSITORY_TYPE;
import static pl.allegro.tech.hermes.domain.topic.schema.SchemaRepositoryType.SCHEMA_REPO;

public class ConsumersStarter implements Starter<HermesConsumers> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumersStarter.class);

    private final MutableConfigFactory configFactory = new MutableConfigFactory();
    private HermesConsumers consumers;

    @Override
    public void start() throws Exception {
        LOGGER.info("Starting Hermes Consumers");
        configFactory.overrideProperty(SCHEMA_REPOSITORY_TYPE, SCHEMA_REPO.name());
        configFactory.overrideProperty(SCHEMA_CACHE_ENABLED, false);
        configFactory.overrideProperty(KAFKA_CONSUMER_AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumers = HermesConsumers.consumers()
            .withKafkaTopicsNamesMapper(serviceLocator ->
                    new IntegrationTestKafkaNamesMapperFactory(configFactory.getStringProperty(Configs.KAFKA_NAMESPACE)).create())
            .withBinding(configFactory, ConfigFactory.class)
            .withBinding(new MultiUrlEndpointAddressResolver(), EndpointAddressResolver.class)
                .withLogRepository(serviceLocator -> new MongoLogRepository(FongoFactory.hermesDB(),
                        10,
                        1000,
                        configFactory.getStringProperty(Configs.KAFKA_CLUSTER_NAME),
                        configFactory.getStringProperty(Configs.HOSTNAME),
                        serviceLocator.getService(MetricRegistry.class),
                        serviceLocator.getService(PathsCompiler.class)))
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
