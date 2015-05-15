package pl.allegro.tech.hermes.common.metric;

import org.junit.Test;

import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;

public class MetricsTest {

    @Test
    public void shouldProduceAppropriatePathForConsumerMetrics() throws UnknownHostException {
        // given
        Metrics.Meter meter = Metrics.Meter.CONSUMER_DISCARDED_METER;

        // when
        assertThat(meter.displayName("tech.hermes"))
                .startsWith("tech.hermes")
                .contains(".consumer.")
                .contains(Metrics.ESCAPED_HOSTNAME)
                .endsWith(".discarded-meter");
    }

    @Test
    public void shouldProduceAppropriatePathForProducerMetrics() throws UnknownHostException {
        // given
        Metrics.Meter meter = Metrics.Meter.PRODUCER_FAILED_METER;

        // when
        assertThat(meter.displayName("tech.hermes"))
                .startsWith("tech.hermes")
                .contains(".producer.")
                .contains(Metrics.ESCAPED_HOSTNAME)
                .endsWith(".failed-meter");
    }

    @Test
    public void shouldEscapeDots() {
        // when & then
        assertThat(Metrics.escapeDots("tech.hermes")).isEqualTo("tech_hermes");
    }

}
