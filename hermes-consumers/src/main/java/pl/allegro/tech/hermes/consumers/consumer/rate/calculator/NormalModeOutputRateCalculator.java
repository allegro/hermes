package pl.allegro.tech.hermes.consumers.consumer.rate.calculator;

import pl.allegro.tech.hermes.consumers.consumer.rate.DeliveryCounters;

class NormalModeOutputRateCalculator implements ModeOutputRateCalculator {

    private final double rateConvergenceFactor;
    private final double slowModeRate;
    private final double failuresRatioThreshold;

    NormalModeOutputRateCalculator(double rateConvergenceFactor, double slowModeRate, double failuresRatioThreshold) {
        this.rateConvergenceFactor = rateConvergenceFactor;
        this.slowModeRate = slowModeRate;
        this.failuresRatioThreshold = failuresRatioThreshold;
    }

    @Override
    public OutputRateCalculationResult calculateOutputRate(double currentRate, double maximumOutputRate, DeliveryCounters counters) {
        double calculatedRate = currentRate;
        OutputRateCalculator.Mode calculatedMode = OutputRateCalculator.Mode.NORMAL;

        if (counters.noFailures() && currentRate < maximumOutputRate) {
            double rateAddOn = (maximumOutputRate - currentRate) * rateConvergenceFactor;
            calculatedRate = Math.min(maximumOutputRate, currentRate + rateAddOn);
        } else if (counters.majorityOfFailures()) {
            calculatedRate = slowModeRate;
            calculatedMode = OutputRateCalculator.Mode.SLOW;
        } else if (counters.failuresRatioExceeds(failuresRatioThreshold)) {
            calculatedRate = Math.max(slowModeRate, currentRate * (1 - rateConvergenceFactor));
        }

        if (calculatedRate <= slowModeRate) {
            calculatedMode = OutputRateCalculator.Mode.SLOW;
        }
        return new OutputRateCalculationResult(calculatedRate, calculatedMode);
    }

}
