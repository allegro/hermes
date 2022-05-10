package pl.allegro.tech.hermes.common.di.factories;

import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicPropertyFactory;
import org.apache.commons.configuration.AbstractConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;

public class ConfigFactoryCreator {

    private static final Logger logger = LoggerFactory.getLogger(ConfigFactoryCreator.class);

    public ConfigFactory provide(AbstractConfiguration abstractConfiguration) {
        DynamicPropertyFactory dynamicPropertyFactory = createDynamicPropertyFactory(abstractConfiguration);
        return new ConfigFactory(dynamicPropertyFactory);
    }

    public void dispose(ConfigFactory instance) {

    }

    private DynamicPropertyFactory createDynamicPropertyFactory(AbstractConfiguration abstractConfiguration) {
        installConfiguration(abstractConfiguration);
        return DynamicPropertyFactory.getInstance();
    }

    private void installConfiguration(AbstractConfiguration configuration) {
        if(ConfigurationManager.isConfigurationInstalled()) {
            logger.warn("Custom Archaius configuration is already installed. " +
                        "Check runtime environment. {}", ConfigurationManager.getConfigInstance());
            return;
        }
        ConfigurationManager.install(configuration);
    }
}
