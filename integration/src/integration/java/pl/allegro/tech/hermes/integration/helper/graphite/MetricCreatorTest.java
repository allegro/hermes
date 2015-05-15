package pl.allegro.tech.hermes.integration.helper.graphite;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MetricCreatorTest {

    @Test
    public void shouldCreateMetric() {
        String data = "hermes.producer.c52940.meter.m1_rate 0.17 1412233849";

        Metric metric = MetricCreator.create(data);

        assertThat(metric.getName()).isEqualTo("hermes.producer.c52940.meter.m1_rate");
        assertThat(metric.getValue()).isEqualTo(0.17);
        assertThat(metric.getTimestamp()).isEqualTo(1412233849);
    }
}