package pl.allegro.tech.hermes.management.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthProblemIndicator;
import pl.allegro.tech.hermes.management.domain.subscription.health.problem.LaggingIndicator;
import pl.allegro.tech.hermes.management.domain.subscription.health.problem.MalfunctioningIndicator;
import pl.allegro.tech.hermes.management.domain.subscription.health.problem.ReceivingMalformedMessagesIndicator;
import pl.allegro.tech.hermes.management.domain.subscription.health.problem.SlowIndicator;
import pl.allegro.tech.hermes.management.domain.subscription.health.problem.TimingOutIndicator;
import pl.allegro.tech.hermes.management.domain.subscription.health.problem.UnreachableIndicator;

@Configuration
@EnableConfigurationProperties({HealthProperties.class})
public class HealthConfiguration {
    @Autowired
    private HealthProperties healthProperties;

    @Bean
    public SubscriptionHealthProblemIndicator laggingIndicator() {
        return new LaggingIndicator(healthProperties.getMaxLagInSeconds());
    }

    @Bean
    public SubscriptionHealthProblemIndicator slowIndicator() {
        return new SlowIndicator(healthProperties.getMinSubscriptionToTopicSpeedRatio());
    }

    @Bean
    public SubscriptionHealthProblemIndicator unreachableIndicator() {
        return new UnreachableIndicator(healthProperties.getMaxOtherErrorsRatio(), healthProperties.getMinSubscriptionRateForReliableMetrics());
    }

    @Bean
    public SubscriptionHealthProblemIndicator timingOutIndicator() {
        return new TimingOutIndicator(healthProperties.getMaxTimeoutsRatio(), healthProperties.getMinSubscriptionRateForReliableMetrics());
    }

    @Bean
    public SubscriptionHealthProblemIndicator malfunctioningIndicator() {
        return new MalfunctioningIndicator(healthProperties.getMax5xxErrorsRatio(), healthProperties.getMinSubscriptionRateForReliableMetrics());
    }

    @Bean
    public SubscriptionHealthProblemIndicator receivingMalformedMessagesIndicator() {
        return new ReceivingMalformedMessagesIndicator(healthProperties.getMax4xxErrorsRatio(), healthProperties.getMinSubscriptionRateForReliableMetrics());
    }
}
