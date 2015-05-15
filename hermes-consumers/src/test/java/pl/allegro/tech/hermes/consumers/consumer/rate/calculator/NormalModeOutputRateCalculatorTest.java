package pl.allegro.tech.hermes.consumers.consumer.rate.calculator;

import org.junit.Test;
import pl.allegro.tech.hermes.consumers.consumer.rate.DeliveryCounters;

import static pl.allegro.tech.hermes.consumers.test.HermesConsumersAssertions.assertThat;

public class NormalModeOutputRateCalculatorTest {

    private static final int SLOW_RATE = 5;

    private final NormalModeOutputRateCalculator calculator = new NormalModeOutputRateCalculator(0.5, SLOW_RATE, 0.4);

    private final DeliveryCounters counters = new DeliveryCounters();

    @Test
    public void shouldIncreaseRateWhenNoFailuresOccuredAndNotAboveMaximumRate() {
        // given
        counters.incrementSuccesses();

        // when then
        assertThat(calculator.calculateOutputRate(10, 20, counters).rate()).isEqualTo(15);
    }

    @Test
    public void shouldNotIncreaseRateWhenNoFailuresOccuredAndAlreadyAtMaximum() {
        // given
        counters.incrementSuccesses();

        // when then
        assertThat(calculator.calculateOutputRate(20, 20, counters)).hasRate(20);
    }

    @Test
    public void shouldNotSlowDownWhenRatioOfFailuresIsBelowGivenThreshold() {
        // given
        NormalModeOutputRateCalculator calculator = new NormalModeOutputRateCalculator(0.5, SLOW_RATE, 0.38);
        counters.incrementSuccesses().incrementSuccesses()
                .incrementFailures();

        // when then
        assertThat(calculator.calculateOutputRate(20, 20, counters)).hasRate(20);
    }

    @Test
    public void shouldSlowDownWhenRatioOfFailuresExceedsGivenThreshold() {
        // given
        NormalModeOutputRateCalculator calculator = new NormalModeOutputRateCalculator(0.5, SLOW_RATE, 0.38);
        counters.incrementSuccesses().incrementSuccesses().incrementSuccesses()
                .incrementFailures().incrementFailures();

        // when then
        assertThat(calculator.calculateOutputRate(20, 20, counters)).hasRate(10);
    }

    @Test
    public void shouldNotLimitRateWhenThereWereNoEvents() {
        assertThat(calculator.calculateOutputRate(20, 20, counters)).hasRate(20);
    }

    @Test
    public void shouldSwitchToSlowModeWhenCalculatedRateIsBelowSlowModeRate() {
        // given
        counters.incrementSuccesses()
                .incrementFailures();

        // when then
        assertThat(calculator.calculateOutputRate(8, 20, counters)).hasRate(SLOW_RATE);
    }

    @Test
    public void shouldSwitchToSlowModeWhenThereWereMoreFailuresThanSuccesses() {
        // given
        counters.incrementSuccesses()
                .incrementFailures().incrementFailures();

        // when then
        assertThat(calculator.calculateOutputRate(10, 20, counters)).hasRate(5).isInMode(OutputRateCalculator.Mode.SLOW);
    }
}
