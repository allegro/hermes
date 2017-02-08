package pl.allegro.tech.hermes.consumers.consumer.rate.calculator;

import org.assertj.core.api.Assertions;
import org.assertj.core.data.Offset;
import pl.allegro.tech.hermes.consumers.consumer.rate.SendCounters;
import pl.allegro.tech.hermes.consumers.consumer.rate.calculator.OutputRateCalculator.Mode;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

public class OutputRateCalculationScenario {

    private final List<OutputRateCalculationResult> results = new ArrayList<>();

    private OutputRateCalculationResult previous;

    private final OutputRateCalculator calculator;

    private final SendCounters counters = new SendCounters(Clock.systemDefaultZone());

    public OutputRateCalculationScenario(OutputRateCalculator calculator) {
        this.calculator = calculator;
    }

    public OutputRateCalculationScenario start(double initialRate) {
        previous = calculator.recalculateRate(counters, OutputRateCalculator.Mode.NORMAL, initialRate);
        results.add(previous);
        return this;
    }

    public OutputRateCalculationScenario nextInteration(int successes, int failures) {
        incrementSuccesses(successes);
        incrementFailures(failures);
        previous = calculator.recalculateRate(counters, previous.mode(), previous.rate());
        results.add(previous);
        counters.reset();
        return this;
    }

    private void incrementSuccesses(int times) {
        for (int i = 0; i < times; ++i) {
            counters.incrementSuccesses();
        }
    }

    private void incrementFailures(int times) {
        for (int i = 0; i < times; ++i) {
            counters.incrementFailures();
        }
    }

    public OutputRateCalculationScenario nextSuccessIteration() {
        return nextInteration(1, 0);
    }

    public OutputRateCalculationScenario nextFailureIteration() {
        return nextInteration(0, 1);
    }

    public OutputRateCalculationScenario fastForwardWithSuccesses(int iterations) {
        for (int i = 0; i < iterations; ++i) {
            nextSuccessIteration();
        }
        return this;
    }

    public OutputRateCalculationScenario fastForwardWithFailures(int iterations) {
        for (int i = 0; i < iterations; ++i) {
            nextFailureIteration();
        }
        return this;
    }

    public void verifyModes(int offset, Mode... modes) {
        for (int i = offset; i < modes.length; ++i) {
            Assertions.assertThat(results.get(i).mode()).isEqualTo(modes[i]);
        }
    }

    public void verifyRates(int offset, double... rates) {
        for (int i = offset; i < rates.length; ++i) {
            Assertions.assertThat(results.get(i).rate()).isEqualTo(rates[i]);
        }
    }

    public OutputRateCalculationScenario verifyIntermediateResult(int iteration, double rate, Mode mode) {
        Assertions.assertThat(results.get(iteration).rate()).isEqualTo(rate, Offset.offset(0.1));
        Assertions.assertThat(results.get(iteration).mode()).isEqualTo(mode);
        return this;
    }

    public OutputRateCalculationScenario verifyFinalResult(double rate, Mode mode) {

        Assertions.assertThat(results.get(results.size() - 1).mode()).isEqualTo(mode);

        double actualRate = results.get(results.size() - 1).rate();
        if (mode == Mode.NORMAL) {
            Assertions.assertThat(actualRate).isEqualTo(rate, Offset.offset(0.1));
        } else {
            Assertions.assertThat(actualRate).isEqualTo(rate);
        }
        return this;
    }
}
