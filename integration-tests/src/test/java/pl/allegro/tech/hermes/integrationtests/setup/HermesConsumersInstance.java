package pl.allegro.tech.hermes.integrationtests.setup;

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

public class HermesConsumersInstance implements Starter<ConfigurableApplicationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HermesConsumersInstance.class);
    private final List<String> args;
    private final SpringApplication application = new SpringApplication(HermesConsumers.class);
    private ConfigurableApplicationContext applicationContext;

    public HermesConsumersInstance() {
        args = new ArrayList<>();
        application.setWebApplicationType(WebApplicationType.NONE);
    }

    @Override
    public void start() throws Exception {
        LOGGER.info("Starting Hermes Consumers");
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
