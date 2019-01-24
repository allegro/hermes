package pl.allegro.tech.hermes.management.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthProblemIndicator;
import pl.allegro.tech.hermes.management.domain.subscription.health.problem.DisabledIndicator;
import pl.allegro.tech.hermes.management.domain.subscription.health.problem.LaggingIndicator;
import pl.allegro.tech.hermes.management.domain.subscription.health.problem.MalfunctioningIndicator;
import pl.allegro.tech.hermes.management.domain.subscription.health.problem.ReceivingMalformedMessagesIndicator;
import pl.allegro.tech.hermes.management.domain.subscription.health.problem.TimingOutIndicator;
import pl.allegro.tech.hermes.management.domain.subscription.health.problem.UnreachableIndicator;

@Configuration
@EnableConfigurationProperties({HealthProperties.class})
public class HealthConfiguration {
    private static final DisabledIndicator DISABLED_INDICATOR = new DisabledIndicator();

    @Autowired
    private HealthProperties healthProperties;

    @Bean
    public SubscriptionHealthProblemIndicator laggingIndicator() {
        if (healthProperties.isLaggingIndicatorEnabled()) {
            return new LaggingIndicator(healthProperties.getMaxLagInSeconds());
        }
        return DISABLED_INDICATOR;
    }

    @Bean
    public SubscriptionHealthProblemIndicator unreachableIndicator() {
        if (healthProperties.isUnreachableIndicatorEnabled()) {
            return new UnreachableIndicator(healthProperties.getMaxOtherErrorsRatio(), healthProperties.getMinSubscriptionRateForReliableMetrics());
        }
        return DISABLED_INDICATOR;
    }

    @Bean
    public SubscriptionHealthProblemIndicator timingOutIndicator() {
        if (healthProperties.isTimingOutIndicatorEnabled()) {
            return new TimingOutIndicator(healthProperties.getMaxTimeoutsRatio(), healthProperties.getMinSubscriptionRateForReliableMetrics());
        }
        return DISABLED_INDICATOR;
    }

    @Bean
    public SubscriptionHealthProblemIndicator malfunctioningIndicator() {
        if (healthProperties.isMalfunctioningIndicatorEnabled()) {
            return new MalfunctioningIndicator(healthProperties.getMax5xxErrorsRatio(), healthProperties.getMinSubscriptionRateForReliableMetrics());
        }
        return DISABLED_INDICATOR;
    }

    @Bean
    public SubscriptionHealthProblemIndicator receivingMalformedMessagesIndicator() {
        if (healthProperties.isReceivingMalformedMessagesIndicatorEnabled()) {
            return new ReceivingMalformedMessagesIndicator(healthProperties.getMax4xxErrorsRatio(), healthProperties.getMinSubscriptionRateForReliableMetrics());
        }
        return DISABLED_INDICATOR;
    }
}
