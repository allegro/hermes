package pl.allegro.tech.hermes.consumers.consumer.rate.calculator;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.consumers.consumer.rate.SendCounters;
import pl.allegro.tech.hermes.consumers.consumer.rate.maxrate.MaxRateProvider;

import java.util.EnumMap;
import java.util.Map;

public class OutputRateCalculator {

    public enum Mode {
        NORMAL,
        SLOW,
        HEARTBEAT
    }

    private final Map<Mode, ModeOutputRateCalculator> modeCalculators = new EnumMap<>(Mode.class);

    private final MaxRateProvider maxRateProvider;

    private final Subscription subscription;

    public OutputRateCalculator(Subscription subscription,
                                ConfigFactory configFactory,
                                MaxRateProvider maxRateProvider) {
        this.subscription = subscription;
        this.maxRateProvider = maxRateProvider;

        modeCalculators.put(Mode.NORMAL,
                new NormalModeOutputRateCalculator(
                        configFactory.getDoubleProperty(Configs.CONSUMER_RATE_CONVERGENCE_FACTOR),
                        1.0 / configFactory.getIntProperty(Configs.CONSUMER_RATE_LIMITER_SLOW_MODE_DELAY),
                        configFactory.getDoubleProperty(Configs.CONSUMER_RATE_FAILURES_SPEEDUP_TOLERANCE_RATIO),
                        configFactory.getDoubleProperty(Configs.CONSUMER_RATE_FAILURES_NOCHANGE_TOLERANCE_RATIO))
        );
        modeCalculators.put(Mode.SLOW,
                new SlowModeOutputRateCalculator(
                        1.0 / configFactory.getIntProperty(Configs.CONSUMER_RATE_LIMITER_HEARTBEAT_MODE_DELAY))
        );
        modeCalculators.put(Mode.HEARTBEAT, new HeartbeatModeOutputRateCalculator(
                1.0 / configFactory.getIntProperty(Configs.CONSUMER_RATE_LIMITER_SLOW_MODE_DELAY))
        );
    }

    public OutputRateCalculationResult recalculateRate(SendCounters counters,
                                                       Mode currentMode, double currentRateLimit) {

        double maximumRate = maxRateProvider.get();
        OutputRateCalculationResult recalculatedResult
                = modeCalculators.get(currentMode).calculateOutputRate(currentRateLimit, maximumRate, counters);

        if (recalculatedResult.rate() > maximumRate) {
            recalculatedResult = OutputRateCalculationResult.adjustRate(recalculatedResult, maximumRate);
        }

        return recalculatedResult;
    }
}
