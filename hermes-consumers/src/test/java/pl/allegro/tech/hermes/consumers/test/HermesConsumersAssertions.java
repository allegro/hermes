package pl.allegro.tech.hermes.consumers.test;

import org.assertj.core.api.Assertions;
import pl.allegro.tech.hermes.consumers.consumer.rate.calculator.OutputRateCalculationResult;
import pl.allegro.tech.hermes.consumers.consumer.rate.calculator.OutputRateCalculationResultAssert;

public class HermesConsumersAssertions extends Assertions {

  public static OutputRateCalculationResultAssert assertThat(OutputRateCalculationResult actual) {
    return OutputRateCalculationResultAssert.assertThat(actual);
  }
}
