package pl.allegro.tech.hermes.integration.env;

import com.codahale.metrics.MetricRegistry;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.consumers.HermesConsumers;
import pl.allegro.tech.hermes.consumers.HermesConsumersApp;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.EndpointAddressResolver;
import pl.allegro.tech.hermes.consumers.di.config.PrimaryBeanCustomizer;
import pl.allegro.tech.hermes.integration.setup.HermesManagementInstance;
import pl.allegro.tech.hermes.metrics.PathsCompiler;
import pl.allegro.tech.hermes.test.helper.config.MutableConfigFactory;
import pl.allegro.tech.hermes.test.helper.endpoint.MultiUrlEndpointAddressResolver;
import pl.allegro.tech.hermes.test.helper.environment.Starter;
import pl.allegro.tech.hermes.tracker.mongo.consumers.MongoLogRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

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
    private final List<String> args = new ArrayList<>();
    private HermesConsumers hermesConsumers;

    @Override
    public void start() throws Exception {
        LOGGER.info("Starting Hermes Consumers");
        overrideProperty(SCHEMA_CACHE_ENABLED, true);
        overrideProperty(KAFKA_CONSUMER_AUTO_OFFSET_RESET_CONFIG, "earliest");
        overrideProperty(KAFKA_CONSUMER_RECONNECT_BACKOFF_MS_CONFIG, 25);
        overrideProperty(KAFKA_CONSUMER_RETRY_BACKOFF_MS_CONFIG, 25);
        overrideProperty(KAFKA_CONSUMER_MAX_POLL_RECORDS_CONFIG, 1);
        overrideProperty(KAFKA_CONSUMER_REQUEST_TIMEOUT_MS_CONFIG, 11000);
        overrideProperty(KAFKA_CONSUMER_SESSION_TIMEOUT_MS_CONFIG, 10000);
        overrideProperty(KAFKA_CONSUMER_HEARTBEAT_INTERVAL_MS_CONFIG, 50);
        overrideProperty(CONSUMER_USE_TOPIC_MESSAGE_SIZE, true);
        overrideProperty(CONSUMER_SSL_KEYSTORE_SOURCE, "provided");
        overrideProperty(CONSUMER_SSL_TRUSTSTORE_SOURCE, "provided");

//        args.add("-p");
//        args.add("" + port);
        args.add("-e");//TODO - do we want to use profiles?
        args.add("integration");

        HermesConsumersApp.main(args.toArray(new String[0]));
        this.hermesConsumers = HermesConsumersApp.getInstance();//TODO?
    }

    @Override
    public HermesConsumers instance() {
        return this.hermesConsumers;
    }

    @Override
    public void stop() throws Exception {
        LOGGER.info("Stopping Hermes Consumers");
        instance().stop();
    }

    public void overrideProperty(Configs config, Object value) {
        args.add("--" + config.getName() + "=" + value);
    }

}
