package pl.allegro.tech.hermes.consumers.consumer.rate.calculator;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

public final class OutputRateCalculationResultAssert
    extends AbstractAssert<OutputRateCalculationResultAssert, OutputRateCalculationResult> {

  private OutputRateCalculationResultAssert(OutputRateCalculationResult actual) {
    super(actual, OutputRateCalculationResultAssert.class);
  }

  public static OutputRateCalculationResultAssert assertThat(OutputRateCalculationResult result) {
    return new OutputRateCalculationResultAssert(result);
  }

  public OutputRateCalculationResultAssert isInMode(OutputRateCalculator.Mode mode) {
    Assertions.assertThat(actual.mode()).isEqualTo(mode);
    return this;
  }

  public OutputRateCalculationResultAssert hasRate(double rate) {
    Assertions.assertThat(actual.rate()).isEqualTo(rate);
    return this;
  }
}
