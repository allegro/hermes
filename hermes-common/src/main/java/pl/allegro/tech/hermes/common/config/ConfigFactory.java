package pl.allegro.tech.hermes.common.config;

import com.netflix.config.DynamicPropertyFactory;//TODO: archaius - ma byc zastapiony Spring cloud

import javax.inject.Inject;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

public class ConfigFactory {

    private final DynamicPropertyFactory propertyFactory;

    @Inject
    public ConfigFactory(DynamicPropertyFactory propertyFactory) {
        this.propertyFactory = propertyFactory;
    }

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
}
