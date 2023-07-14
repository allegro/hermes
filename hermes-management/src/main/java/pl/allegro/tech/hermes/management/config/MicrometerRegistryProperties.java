package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.common.di.factories.MicrometerRegistryParameters;

import java.util.List;

@ConfigurationProperties(prefix = "metrics.micrometer")
public class MicrometerRegistryProperties implements MicrometerRegistryParameters {

    private List<Double> percentiles = List.of(0.5, 0.99, 0.999);

    @Override
    public List<Double> getPercentiles() {
        return percentiles;
    }

    public void setPercentiles(List<Double> percentiles) {
        this.percentiles = percentiles;
    }
}
