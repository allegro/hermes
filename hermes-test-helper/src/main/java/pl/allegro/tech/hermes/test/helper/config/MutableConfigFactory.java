package pl.allegro.tech.hermes.test.helper.config;

import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;

import java.util.HashMap;
import java.util.Map;

public class MutableConfigFactory extends ConfigFactory {

    private Map<Configs, Object> overridden = new HashMap<>();

    @Override
    public String getIntPropertyAsString(Configs config) {
        return overridden.containsKey(config) ? (String) overridden.get(config) : super.getIntPropertyAsString(config);
    }

    @Override
    public int getIntProperty(Configs config) {
        return overridden.containsKey(config) ? (Integer) overridden.get(config) : super.getIntProperty(config);
    }

    @Override
    public long getLongProperty(Configs config) {
        return overridden.containsKey(config) ? (Long) overridden.get(config) : super.getLongProperty(config);
    }

    @Override
    public String getStringProperty(Configs config) {
        return overridden.containsKey(config) ? (String) overridden.get(config) : super.getStringProperty(config);
    }

    @Override
    public double getDoubleProperty(Configs config) {
        return overridden.containsKey(config) ? (Double) overridden.get(config) : super.getDoubleProperty(config);
    }

    @Override
    public boolean getBooleanProperty(Configs config) {
        return overridden.containsKey(config) ? (Boolean) overridden.get(config) : super.getBooleanProperty(config);
    }

    public MutableConfigFactory overrideProperty(Configs property, Object value) {
        overridden.put(property, value);
        return this;
    }

}
