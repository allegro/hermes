package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.common.di.factories.MicrometerRegistryParameters;


@ConfigurationProperties(prefix = "consumer.metrics.micrometer")
public class MicrometerRegistryProperties implements MicrometerRegistryParameters {

    private String disabledAttributes = "M15_RATE, M5_RATE, MEAN, MEAN_RATE, MIN, STDDEV";

    @Override
    public String getDisabledAttributes() {
        return disabledAttributes;
    }

    public void setDisabledAttributes(String disabledAttributes) {
        this.disabledAttributes = disabledAttributes;
    }
}
