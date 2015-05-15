package pl.allegro.tech.hermes.integration.env;

import com.mongodb.DB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.consumers.HermesConsumers;
import pl.allegro.tech.hermes.test.helper.environment.Starter;

public class ConsumersStarter implements Starter<HermesConsumers> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumersStarter.class);

    private final MutableConfigFactory configFactory = new MutableConfigFactory();
    private HermesConsumers consumers;

    @Override
    public void start() throws Exception {
        LOGGER.info("Starting Hermes Consumers");
        consumers = HermesConsumers.consumers()
            .withBinding(new FongoFactory().provide(), DB.class)
            .withBinding(configFactory, ConfigFactory.class)
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
