package pl.allegro.tech.hermes.common.config;

import com.netflix.config.DynamicPropertyFactory;

public class ConfigFactory {

    private final DynamicPropertyFactory propertyFactory = DynamicPropertyFactory.getInstance();

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

}
