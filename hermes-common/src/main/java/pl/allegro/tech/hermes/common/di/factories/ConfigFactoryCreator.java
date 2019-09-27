package pl.allegro.tech.hermes.common.di.factories;

import com.netflix.config.AbstractPollingScheduler;
import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicConfiguration;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.sources.URLConfigurationSource;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.EnvironmentConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.glassfish.hk2.api.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;

import static com.netflix.config.ConfigurationManager.APPLICATION_PROPERTIES;
import static com.netflix.config.ConfigurationManager.DISABLE_DEFAULT_ENV_CONFIG;
import static com.netflix.config.ConfigurationManager.DISABLE_DEFAULT_SYS_CONFIG;
import static com.netflix.config.ConfigurationManager.ENV_CONFIG_NAME;
import static com.netflix.config.ConfigurationManager.SYS_CONFIG_NAME;
import static com.netflix.config.ConfigurationManager.URL_CONFIG_NAME;

public class ConfigFactoryCreator implements Factory<ConfigFactory> {

    private static final Logger logger = LoggerFactory.getLogger(ConfigFactoryCreator.class);

    private static final String DISABLE_CONFIG_POLLING_SCHEDULER = "archaius.fixedDelayPollingScheduler.disabled";

    @Override
    public ConfigFactory provide() {
        Boolean isConfigPollingSchedulerDisabled = Boolean.valueOf(System.getProperty(DISABLE_CONFIG_POLLING_SCHEDULER, "true"));
        DynamicPropertyFactory dynamicPropertyFactory = createDynamicPropertyFactory(isConfigPollingSchedulerDisabled);
        return new ConfigFactory(dynamicPropertyFactory);
    }

    @Override
    public void dispose(ConfigFactory instance) {

    }

    private DynamicPropertyFactory createDynamicPropertyFactory(boolean isConfigPollingSchedulerDisabled) {
        if (isConfigPollingSchedulerDisabled) {
            ConfigurationManager.install(createConfigInstance());
        }
        return DynamicPropertyFactory.getInstance();
    }

    private static AbstractConfiguration createConfigInstance() {
        ConcurrentCompositeConfiguration config = new ConcurrentCompositeConfiguration();
        try {
            DynamicConfiguration urlConfig = new DynamicConfiguration(new URLConfigurationSource(), createDisabledPollingScheduler());
            config.addConfiguration(urlConfig, URL_CONFIG_NAME);
        } catch (Throwable e) {
            logger.warn("Failed to create default dynamic configuration", e);
        }
        if (!Boolean.getBoolean(DISABLE_DEFAULT_SYS_CONFIG)) {
            SystemConfiguration sysConfig = new SystemConfiguration();
            config.addConfiguration(sysConfig, SYS_CONFIG_NAME);
        }
        if (!Boolean.getBoolean(DISABLE_DEFAULT_ENV_CONFIG)) {
            EnvironmentConfiguration envConfig = new EnvironmentConfiguration();
            config.addConfiguration(envConfig, ENV_CONFIG_NAME);
        }
        ConcurrentCompositeConfiguration appOverrideConfig = new ConcurrentCompositeConfiguration();
        config.addConfiguration(appOverrideConfig, APPLICATION_PROPERTIES);
        config.setContainerConfigurationIndex(config.getIndexOfConfiguration(appOverrideConfig));
        return config;
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
