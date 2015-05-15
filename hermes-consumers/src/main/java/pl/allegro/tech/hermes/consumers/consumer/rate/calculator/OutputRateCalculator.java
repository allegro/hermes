package pl.allegro.tech.hermes.consumers.consumer.rate.calculator;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.rate.DeliveryCounters;

import javax.inject.Inject;
import java.util.EnumMap;
import java.util.Map;

public class OutputRateCalculator {

    public enum Mode {

        NORMAL,
        SLOW,
        HEARTBEAT
    }

    private final Map<Mode, ModeOutputRateCalculator> modeCalculators
            = new EnumMap<>(Mode.class);

    private final MaximumOutputRateCalculator maximumRateCalculator;

    @Inject
    public OutputRateCalculator(ConfigFactory configFactory, HermesMetrics hermesMetrics) {
        modeCalculators.put(Mode.NORMAL,
                new NormalModeOutputRateCalculator(
                        configFactory.getDoubleProperty(Configs.CONSUMER_RATE_CONVERGENCE_FACTOR),
                        1.0 / configFactory.getIntProperty(Configs.CONSUMER_RATE_LIMITER_SLOW_MODE_DELAY),
                        configFactory.getDoubleProperty(Configs.CONSUMER_RATE_FAILURES_RATIO_THRESHOLD))
        );
        modeCalculators.put(Mode.SLOW,
                new SlowModeOutputRateCalculator(
                        1.0 / configFactory.getIntProperty(Configs.CONSUMER_RATE_LIMITER_HEARTBEAT_MODE_DELAY))
        );
        modeCalculators.put(Mode.HEARTBEAT, new HeartbeatModeOutputRateCalculator(
                1.0 / configFactory.getIntProperty(Configs.CONSUMER_RATE_LIMITER_SLOW_MODE_DELAY))
        );

        maximumRateCalculator = new MaximumOutputRateCalculator(hermesMetrics);
    }

    public OutputRateCalculationResult recalculateRate(Subscription subscription, DeliveryCounters counters,
            Mode currentMode, double currentRate) {
        double maximumRate = maximumRateCalculator.calculateMaximumOutputRate(subscription);
        OutputRateCalculationResult recalculatedResult
                = modeCalculators.get(currentMode).calculateOutputRate(currentRate, maximumRate, counters);

        if (recalculatedResult.rate() > maximumRate) {
            recalculatedResult = OutputRateCalculationResult.adjustRate(recalculatedResult, maximumRate);
        }

        return recalculatedResult;
    }
}
