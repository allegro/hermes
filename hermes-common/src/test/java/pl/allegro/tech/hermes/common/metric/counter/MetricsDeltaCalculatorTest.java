package pl.allegro.tech.hermes.common.metric.counter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class MetricsDeltaCalculatorTest {

  private final MetricsDeltaCalculator calculator = new MetricsDeltaCalculator();

  @Test
  public void shouldReturnCurrentValueWhenThereIsNoPreviousStateForMetric() {
    // given when
    long delta = calculator.calculateDelta("metricWithoutHistory", 13L);

    // then
    assertThat(delta).isEqualTo(13);
  }

  @Test
  public void shouldReturnDeltaBetweenCurrentAndPreviousState() {
    // given
    calculator.calculateDelta("metric", 13L);

    // when
    long delta = calculator.calculateDelta("metric", 20L);

    // then
    assertThat(delta).isEqualTo(7);
  }

  @Test
  public void shouldRevertDelta() {
    // given
    String metricName = "metricToRevert";
    calculator.calculateDelta(metricName, 15L);
    long delta = calculator.calculateDelta(metricName, 20L);

    // when
    calculator.revertDelta(metricName, delta);

    assertThat(calculator.calculateDelta(metricName, 20L)).isEqualTo(5L);
  }

  @Test
  public void shouldNotRevertDeltaForNonExistingMetric() {
    // when
    calculator.revertDelta("emptyMetric", 10L);

    // then
    assertThat(calculator.calculateDelta("emptyMetric", 10L)).isEqualTo(10L);
  }
}
