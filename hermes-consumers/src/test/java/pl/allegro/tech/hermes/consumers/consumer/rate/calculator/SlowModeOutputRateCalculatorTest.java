package pl.allegro.tech.hermes.consumers.consumer.rate.calculator;

import org.junit.Test;
import pl.allegro.tech.hermes.consumers.consumer.rate.SendCounters;

import java.time.Clock;

import static pl.allegro.tech.hermes.consumers.test.HermesConsumersAssertions.assertThat;

public class SlowModeOutputRateCalculatorTest {

    private static final double HEARTBEAT_RATE = 1;

    private final SlowModeOutputRateCalculator calculator = new SlowModeOutputRateCalculator(HEARTBEAT_RATE);

    private final SendCounters counters = new SendCounters(Clock.systemDefaultZone());

    @Test
    public void shouldStayInSlowModeIfThereWereSomeFailures() {
        // given
        counters.incrementSuccesses()
                .incrementFailures();

        // when then
        assertThat(calculator.calculateOutputRate(10, 20, counters)).hasRate(10).isInMode(OutputRateCalculator.Mode.SLOW);
    }

    @Test
    public void shouldSwitchToNormalModeWithoutModifyingRateWhenOnlySuccesses() {
        // given
        counters.incrementSuccesses();

        // when then
        assertThat(calculator.calculateOutputRate(10, 20, counters)).hasRate(10).isInMode(OutputRateCalculator.Mode.NORMAL);
    }

    @Test
    public void shouldSwitchToHeartbeatModeWhenThereWereOnlyFailures() {
        // given
        counters.incrementFailures();

        // when then
        assertThat(calculator.calculateOutputRate(10, 20, counters)).hasRate(HEARTBEAT_RATE).isInMode(OutputRateCalculator.Mode.HEARTBEAT);
    }

}
