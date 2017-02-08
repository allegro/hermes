package pl.allegro.tech.hermes.consumers.consumer.rate.calculator;

import pl.allegro.tech.hermes.consumers.consumer.rate.SendCounters;

class SlowModeOutputRateCalculator implements ModeOutputRateCalculator {

    private final double heartbeatModeRate;

    SlowModeOutputRateCalculator(double heartbeatModeRate) {
        this.heartbeatModeRate = heartbeatModeRate;
    }

    @Override
    public OutputRateCalculationResult calculateOutputRate(double currentRate,
                                                           double maximumOutputRate,
                                                           SendCounters counters) {
        if (counters.majorityOfFailures()) {
            return new OutputRateCalculationResult(heartbeatModeRate, OutputRateCalculator.Mode.HEARTBEAT);
        } else if (counters.onlySuccessess()) {
            return new OutputRateCalculationResult(currentRate, OutputRateCalculator.Mode.NORMAL);
        }
        return new OutputRateCalculationResult(currentRate, OutputRateCalculator.Mode.SLOW);
    }

}
