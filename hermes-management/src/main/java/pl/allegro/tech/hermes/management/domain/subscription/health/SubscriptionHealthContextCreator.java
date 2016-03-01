package pl.allegro.tech.hermes.management.domain.subscription.health;

import pl.allegro.tech.hermes.api.Subscription;

import static java.lang.Double.parseDouble;

class SubscriptionHealthContextCreator {
    public SubscriptionHealthContext createContext(Subscription subscription, pl.allegro.tech.hermes.api.TopicMetrics topicMetrics,
                                                   pl.allegro.tech.hermes.api.SubscriptionMetrics subscriptionMetrics) {
        return new SubscriptionHealthContext(
                subscription,
                convertTopicMetrics(topicMetrics),
                convertSubscriptionMetrics(subscriptionMetrics)
        );
    }

    private TopicMetrics convertTopicMetrics(pl.allegro.tech.hermes.api.TopicMetrics topicMetrics) {
        double rate = parseDouble(topicMetrics.getRate());
        return new TopicMetrics(rate);
    }

    private SubscriptionMetrics convertSubscriptionMetrics(pl.allegro.tech.hermes.api.SubscriptionMetrics subscriptionMetrics) {
        double rate = parseDouble(subscriptionMetrics.getRate());
        double timeoutsRate = parseDouble(subscriptionMetrics.getTimeouts());
        double otherErrorsRate = parseDouble(subscriptionMetrics.getOtherErrors());
        double code4xxErrorsRate = parseDouble(subscriptionMetrics.getCodes4xx());
        double code5xxErrorsRate = parseDouble(subscriptionMetrics.getCodes5xx());
        long lag = subscriptionMetrics.getLag();
        return new SubscriptionMetrics(rate, timeoutsRate, otherErrorsRate, code4xxErrorsRate, code5xxErrorsRate, lag);
    }
}
