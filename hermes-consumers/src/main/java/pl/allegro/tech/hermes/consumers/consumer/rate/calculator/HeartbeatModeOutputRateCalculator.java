package pl.allegro.tech.hermes.consumers.consumer.rate.calculator;

import pl.allegro.tech.hermes.consumers.consumer.rate.SendCounters;

class HeartbeatModeOutputRateCalculator implements ModeOutputRateCalculator {

    private final double slowModeRate;

    HeartbeatModeOutputRateCalculator(double slowModeRate) {
        this.slowModeRate = slowModeRate;
    }

    @Override
    public OutputRateCalculationResult calculateOutputRate(double currentRate,
                                                           double maximumOutputRate,
                                                           SendCounters counters) {
        if (counters.onlySuccessess()) {
            return new OutputRateCalculationResult(slowModeRate, OutputRateCalculator.Mode.SLOW);
        }
        return new OutputRateCalculationResult(currentRate, OutputRateCalculator.Mode.HEARTBEAT);
    }
}
