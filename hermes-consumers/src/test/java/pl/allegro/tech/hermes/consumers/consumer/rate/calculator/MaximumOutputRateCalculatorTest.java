package pl.allegro.tech.hermes.consumers.consumer.rate.calculator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionPolicy;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;

@RunWith(MockitoJUnitRunner.class)
public class MaximumOutputRateCalculatorTest {

    @Mock
    private HermesMetrics metrics;

    private MaximumOutputRateCalculator calculator;

    @Before
    public void setup() {
        this.calculator = new MaximumOutputRateCalculator(metrics);
    }

    @Test
    public void shouldCalculateMaximumConsumerRateAsPartOfOverallSubscriptionRateLimit() {
        // given
        Subscription subscription = subscription("group.topic", "subscription")
                .withSubscriptionPolicy(
                        SubscriptionPolicy.Builder.subscriptionPolicy()
                                .applyDefaults().withRate(1000).build()
                ).build();
        when(metrics.countActiveConsumers(subscription)).thenReturn(4);

        // when then
        assertThat(calculator.calculateMaximumOutputRate(subscription)).isEqualTo(250D);
    }

}
