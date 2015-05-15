package pl.allegro.tech.hermes.consumers.consumer.rate.calculator;

public class OutputRateCalculationResult {

    private final double rate;

    private final OutputRateCalculator.Mode mode;

    public OutputRateCalculationResult(double rate, OutputRateCalculator.Mode mode) {
        this.rate = rate;
        this.mode = mode;
    }

    public static OutputRateCalculationResult adjustRate(OutputRateCalculationResult result, double adjustedRate) {
        return new OutputRateCalculationResult(adjustedRate, result.mode());
    }

    public OutputRateCalculator.Mode mode() {
        return mode;
    }

    public double rate() {
        return rate;
    }

}
