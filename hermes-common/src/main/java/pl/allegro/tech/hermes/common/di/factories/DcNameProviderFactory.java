package pl.allegro.tech.hermes.common.di.factories;

import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.dc.DcNameProvider;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.dc.DcNameSource;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.dc.DefaultDcNameProvider;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.dc.EnvironmentVariableDcNameProvider;

import javax.inject.Inject;

public class DcNameProviderFactory implements Factory<DcNameProvider> {

    private final ConfigFactory configFactory;

    @Inject
    public DcNameProviderFactory(ConfigFactory configFactory) {
        this.configFactory = configFactory;
    }

    @Override
    public DcNameProvider provide() {
        String dcNameSource = configFactory.getStringProperty(Configs.DC_NAME_SOURCE);
        if(DcNameSource.ENV == dcNameSourceFromString(dcNameSource)) {
            String variableName = configFactory.getStringProperty(Configs.DC_NAME_SOURCE_ENV);
            return new EnvironmentVariableDcNameProvider(variableName);
        } else {
            return new DefaultDcNameProvider();
        }
    }

    private DcNameSource dcNameSourceFromString(String value) {
        try {
            return DcNameSource.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void dispose(DcNameProvider instance) {}
}
