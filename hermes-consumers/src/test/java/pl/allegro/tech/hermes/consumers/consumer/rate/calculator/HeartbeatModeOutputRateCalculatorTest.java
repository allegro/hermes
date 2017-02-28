package pl.allegro.tech.hermes.consumers.consumer.rate.calculator;

import org.junit.Test;
import pl.allegro.tech.hermes.consumers.consumer.rate.SendCounters;

import java.time.Clock;

import static pl.allegro.tech.hermes.consumers.test.HermesConsumersAssertions.assertThat;

public class HeartbeatModeOutputRateCalculatorTest {

    private static final int SLOW_RATE = 10;

    private final HeartbeatModeOutputRateCalculator calculator = new HeartbeatModeOutputRateCalculator(SLOW_RATE);

    private final SendCounters counters = new SendCounters(Clock.systemDefaultZone());

    @Test
    public void shouldNotChangeAnythingIfThereWereAnyFailures() {
        // given
        counters.incrementSuccesses()
                .incrementFailures();

        // when then
        assertThat(calculator.calculateOutputRate(1, 20, counters)).hasRate(1).isInMode(OutputRateCalculator.Mode.HEARTBEAT);
    }

    @Test
    public void shouldSwitchToSlowModeIfOnlySuccesses() {
        // given
        counters.incrementSuccesses();

        // when then
        assertThat(calculator.calculateOutputRate(1, 20, counters)).hasRate(SLOW_RATE).isInMode(OutputRateCalculator.Mode.SLOW);
    }
}
