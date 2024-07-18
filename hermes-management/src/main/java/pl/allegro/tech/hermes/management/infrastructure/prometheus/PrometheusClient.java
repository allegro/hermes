package pl.allegro.tech.hermes.management.infrastructure.prometheus;

import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringMetricsContainer;

import java.util.List;


public interface PrometheusClient {
    String SUBSCRIPTION_QUERY_FORMAT = "sum by (group, topic, subscription, status_code)"
            + " (irate({__name__=~'%s', group='%s', topic='%s', subscription='%s', %s}[1m]))";

    String TOPIC_QUERY_FORMAT = "sum by (group, topic) (irate({__name__=~'%s', group='%s', "
            + "topic='%s', %s}[1m]))";

    MonitoringMetricsContainer readMetrics(List<Query> query);

    record Query(String name, String fullQuery) {

        public static Query forSubscription(String name, SubscriptionName subscriptionName, String additionalFilters) {
            String fullQueryName = String.format(SUBSCRIPTION_QUERY_FORMAT, name,
                    subscriptionName.getTopicName().getGroupName(), subscriptionName.getTopicName().getName(),
                    subscriptionName.getName(), additionalFilters);
            return new Query(name, fullQueryName);
        }

        static Query forTopic(String name, TopicName topicName, String additionalFilters) {
            String fullQueryName = String.format(TOPIC_QUERY_FORMAT, name,
                    topicName.getGroupName(), topicName.getName(), additionalFilters);
            return new Query(name, fullQueryName);
        }
    }
}
