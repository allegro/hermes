package pl.allegro.tech.hermes.common.config;

import com.netflix.config.AbstractPollingScheduler;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicConfiguration;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.PolledConfigurationSource;
import com.netflix.config.sources.URLConfigurationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

public class ConfigFactory {

    private static final Logger logger = LoggerFactory.getLogger(ConfigFactory.class);

    private final DynamicPropertyFactory propertyFactory = getDynamicPropertyFactory();

    public String getIntPropertyAsString(Configs config) {
        return String.format("%s", getIntProperty(config));
    }

    public int getIntProperty(Configs config) {
        return propertyFactory.getIntProperty(config.getName(), config.<Integer>getDefaultValue()).get();
    }

    public long getLongProperty(Configs config) {
        return propertyFactory.getLongProperty(config.getName(), config.<Long>getDefaultValue()).get();
    }

    public String getStringProperty(Configs config) {
        return propertyFactory.getStringProperty(config.getName(), config.<String>getDefaultValue()).get();
    }

    public double getDoubleProperty(Configs config) {
        return propertyFactory.getDoubleProperty(config.getName(), config.<Double>getDefaultValue()).get();
    }

    public boolean getBooleanProperty(Configs config) {
        return propertyFactory.getBooleanProperty(config.getName(), config.<Boolean>getDefaultValue()).get();
    }

    public String print(Configs... options) {
        return stream(options).map(opt -> opt.getName() + "=" + getProperty(opt)).collect(joining(", "));
    }

    private String getProperty(Configs opt) {
        return propertyFactory.getContextualProperty(opt.getName(), opt.getDefaultValue()).getValue().toString();
    }

    private DynamicPropertyFactory getDynamicPropertyFactory() {
        if (!ConfigurationManager.isConfigurationInstalled()) {
            PolledConfigurationSource source = new URLConfigurationSource();
            AbstractPollingScheduler scheduler = createDisabledPollingScheduler();
            ConfigurationManager.install(new DynamicConfiguration(source, scheduler));
        }
        return DynamicPropertyFactory.getInstance();
    }

    private static AbstractPollingScheduler createDisabledPollingScheduler() {
        return new AbstractPollingScheduler() {
            @Override
            protected void schedule(Runnable pollingRunnable) {
                logger.info("Periodical polling of a configuration source is turned off!");
            }

            @Override
            public void stop() {

            }
        };
    }
}
