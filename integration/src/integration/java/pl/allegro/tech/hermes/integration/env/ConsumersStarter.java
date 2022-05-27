package pl.allegro.tech.hermes.integration.env;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.consumers.HermesConsumers;
import pl.allegro.tech.hermes.test.helper.environment.Starter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_USE_TOPIC_MESSAGE_SIZE;
import static pl.allegro.tech.hermes.common.config.Configs.SCHEMA_CACHE_ENABLED;
import static pl.allegro.tech.hermes.consumers.ConsumerConfigurationProperties.KAFKA_CONSUMER_AUTO_OFFSET_RESET_CONFIG;
import static pl.allegro.tech.hermes.consumers.ConsumerConfigurationProperties.KAFKA_CONSUMER_HEARTBEAT_INTERVAL_MS_CONFIG;
import static pl.allegro.tech.hermes.consumers.ConsumerConfigurationProperties.KAFKA_CONSUMER_MAX_POLL_RECORDS_CONFIG;
import static pl.allegro.tech.hermes.consumers.ConsumerConfigurationProperties.KAFKA_CONSUMER_RECONNECT_BACKOFF_MS_CONFIG;
import static pl.allegro.tech.hermes.consumers.ConsumerConfigurationProperties.KAFKA_CONSUMER_REQUEST_TIMEOUT_MS_CONFIG;
import static pl.allegro.tech.hermes.consumers.ConsumerConfigurationProperties.KAFKA_CONSUMER_RETRY_BACKOFF_MS_CONFIG;
import static pl.allegro.tech.hermes.consumers.ConsumerConfigurationProperties.KAFKA_CONSUMER_SESSION_TIMEOUT_MS_CONFIG;

public class ConsumersStarter implements Starter<ConfigurableApplicationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumersStarter.class);
    private final List<String> args;
    private final SpringApplication application = new SpringApplication(HermesConsumers.class);
    private ConfigurableApplicationContext applicationContext;

    public ConsumersStarter() {
        args = new ArrayList<>();
        application.setWebApplicationType(WebApplicationType.NONE);
    }

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
        overrideProperty("consumer.ssl.keystoreSource", "provided");
        overrideProperty("consumer.ssl.truststoreSource", "provided");
        overrideProperty("consumer.commit.offset.queuesInflightDrainFullEnabled", true);
        overrideProperty("consumer.commit.offset.period", 1);
        setSpringProfiles("integration");

        applicationContext = application.run(args.toArray(new String[0]));
    }

    @Override
    public ConfigurableApplicationContext instance() {
        return this.applicationContext;
    }

    @Override
    public void stop() throws Exception {
        LOGGER.info("Stopping Hermes Consumers");
        instance().close();
    }

    public void overrideProperty(String config, Object value) {
        args.add("--" + config + "=" + value);
    }

    public void overrideProperty(Configs config, Object value) {
        args.add("--" + config.getName() + "=" + value);
    }

    public void overrideProperty(String configName, Object value) {
        args.add("--" + configName + "=" + value);
    }

    public void setSpringProfiles(String... profiles) {
        String profilesString = Arrays.stream(profiles).collect(Collectors.joining(",", "", ""));
        args.add("--spring.profiles.active=" + profilesString);
    }

}
