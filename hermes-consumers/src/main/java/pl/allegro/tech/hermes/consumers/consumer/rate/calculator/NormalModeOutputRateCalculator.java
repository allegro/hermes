package pl.allegro.tech.hermes.consumers.consumer.rate.calculator;

import pl.allegro.tech.hermes.consumers.consumer.rate.SendCounters;

class NormalModeOutputRateCalculator implements ModeOutputRateCalculator {

    private final double rateConvergenceFactor;
    private final double slowModeRate;
    private final double failuresSpeedupToleranceRatio;
    private final double failuresNochangeToleranceRatio;

    NormalModeOutputRateCalculator(double rateConvergenceFactor, 
                                   double slowModeRate,
                                   double failuresSpeedupToleranceRatio,
                                   double failuresNochangeToleranceRatio) {
        this.rateConvergenceFactor = rateConvergenceFactor;
        this.slowModeRate = slowModeRate;
        this.failuresSpeedupToleranceRatio = failuresSpeedupToleranceRatio;
        this.failuresNochangeToleranceRatio = failuresNochangeToleranceRatio;
    }

    @Override
    public OutputRateCalculationResult calculateOutputRate(double currentRate,
                                                           double maximumOutputRate,
                                                           SendCounters counters) {
        double calculatedRate = currentRate;
        OutputRateCalculator.Mode calculatedMode = OutputRateCalculator.Mode.NORMAL;

        if (!counters.failuresRatioExceeds(failuresSpeedupToleranceRatio) && currentRate < maximumOutputRate) {
            double rateAddOn = (maximumOutputRate - currentRate) * rateConvergenceFactor;
            calculatedRate = Math.min(maximumOutputRate, currentRate + rateAddOn);
        } else if (counters.majorityOfFailures()) {
            calculatedRate = slowModeRate;
            calculatedMode = OutputRateCalculator.Mode.SLOW;
        } else if (counters.failuresRatioExceeds(failuresNochangeToleranceRatio)) {
            calculatedRate = Math.max(slowModeRate, currentRate * (1 - rateConvergenceFactor));
        }

        if (calculatedRate <= slowModeRate) {
            calculatedMode = OutputRateCalculator.Mode.SLOW;
        }
        return new OutputRateCalculationResult(calculatedRate, calculatedMode);
    }

}
