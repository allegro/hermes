package pl.allegro.tech.hermes.integration.env;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import pl.allegro.tech.hermes.consumers.HermesConsumers;
import pl.allegro.tech.hermes.test.helper.environment.Starter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static pl.allegro.tech.hermes.consumers.ConsumerConfigurationProperties.CONSUMER_COMMIT_OFFSET_PERIOD;
import static pl.allegro.tech.hermes.consumers.ConsumerConfigurationProperties.CONSUMER_COMMIT_OFFSET_QUEUES_INFLIGHT_DRAIN_FULL;
import static pl.allegro.tech.hermes.consumers.ConsumerConfigurationProperties.CONSUMER_SSL_KEYSTORE_SOURCE;
import static pl.allegro.tech.hermes.consumers.ConsumerConfigurationProperties.CONSUMER_SSL_TRUSTSTORE_SOURCE;
import static pl.allegro.tech.hermes.consumers.ConsumerConfigurationProperties.CONSUMER_USE_TOPIC_MESSAGE_SIZE;
import static pl.allegro.tech.hermes.consumers.ConsumerConfigurationProperties.SCHEMA_CACHE_ENABLED;

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
        overrideProperty(CONSUMER_USE_TOPIC_MESSAGE_SIZE, true);
        overrideProperty(CONSUMER_SSL_KEYSTORE_SOURCE, "provided");
        overrideProperty(CONSUMER_SSL_TRUSTSTORE_SOURCE, "provided");
        overrideProperty(CONSUMER_COMMIT_OFFSET_QUEUES_INFLIGHT_DRAIN_FULL, true);
        overrideProperty(CONSUMER_COMMIT_OFFSET_PERIOD, "1s");
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

    public void setSpringProfiles(String... profiles) {
        String profilesString = Arrays.stream(profiles).collect(Collectors.joining(",", "", ""));
        args.add("--spring.profiles.active=" + profilesString);
    }

}
