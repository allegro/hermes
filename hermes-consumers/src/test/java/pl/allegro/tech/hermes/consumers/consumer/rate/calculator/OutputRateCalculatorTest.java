package pl.allegro.tech.hermes.consumers.consumer.rate.calculator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionPolicy;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.consumers.consumer.rate.maxrate.MaxRateProvider;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;

@RunWith(MockitoJUnitRunner.class)
public class OutputRateCalculatorTest {

    private static final double HEARTBEAT_RATE = 1.0 / 60.0;

    private static final double MAX_RATE = 100;

    private Subscription subscription;

    private OutputRateCalculator calculator;

    @Before
    public void setup() {
        ConfigFactory config = mock(ConfigFactory.class);
        when(config.getDoubleProperty(Configs.CONSUMER_RATE_CONVERGENCE_FACTOR)).thenReturn(0.5);
        when(config.getIntProperty(Configs.CONSUMER_RATE_LIMITER_SLOW_MODE_DELAY)).thenReturn(1);
        when(config.getIntProperty(Configs.CONSUMER_RATE_LIMITER_HEARTBEAT_MODE_DELAY)).thenReturn(60);

        subscription = subscription("group.topic", "subscription").withSubscriptionPolicy(
                SubscriptionPolicy.Builder.subscriptionPolicy().withRate(200).build()
        ).build();

        MaxRateProvider maxRateProvider = mock(MaxRateProvider.class);
        when(maxRateProvider.get()).thenReturn(100D);

        calculator = new OutputRateCalculator(subscription, config, maxRateProvider);
    }

    @Test
    public void shouldStartInNormalModeAndGraduallyIncreaseToMaximumWhenOnlySuccessfulDeliveries() {
        //given
        OutputRateCalculationScenario scenario = new OutputRateCalculationScenario(calculator);

        // when
        scenario.start(10).fastForwardWithSuccesses(10);

        // then
        scenario.verifyFinalResult(MAX_RATE, OutputRateCalculator.Mode.NORMAL);
    }

    @Test
    public void shouldStartInNormalModeReachMaximumAndGoToHeartbeatViaSlowWhenDeliveryStartsFailing() {
        // given
        OutputRateCalculationScenario scenario = new OutputRateCalculationScenario(calculator);

        // when
        scenario.start(10).fastForwardWithSuccesses(10).nextInteration(2, 10).fastForwardWithFailures(20);

        // then
        scenario.verifyIntermediateResult(11, 1, OutputRateCalculator.Mode.SLOW)
                .verifyFinalResult(HEARTBEAT_RATE, OutputRateCalculator.Mode.HEARTBEAT);
    }

    @Test
    public void shouldRecoverFromHeartbeatModeAndGetToMaximumViaSlowWhenDeliveringWithSuccess() {
        // given
        OutputRateCalculationScenario scenario = new OutputRateCalculationScenario(calculator);

        // when
        scenario.start(10).fastForwardWithFailures(10)
                .fastForwardWithSuccesses(15);

        // then
        scenario.verifyIntermediateResult(11, 1, OutputRateCalculator.Mode.SLOW)
                .verifyFinalResult(MAX_RATE, OutputRateCalculator.Mode.NORMAL);
    }

    @Test
    public void shouldImmediatelySwitchToHeartbeatModeWhenAllMessagesFail() {
        // given
        OutputRateCalculationScenario scenario = new OutputRateCalculationScenario(calculator);

        // when
        scenario.start(10).fastForwardWithSuccesses(10).fastForwardWithFailures(2);

        // then
        scenario.verifyIntermediateResult(10, MAX_RATE, OutputRateCalculator.Mode.NORMAL)
                .verifyFinalResult(HEARTBEAT_RATE, OutputRateCalculator.Mode.HEARTBEAT);
    }

}
