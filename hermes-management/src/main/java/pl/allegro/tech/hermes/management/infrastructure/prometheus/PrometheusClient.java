package pl.allegro.tech.hermes.management.infrastructure.prometheus;

import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.management.infrastructure.metrics.MetricsQuery;
import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringMetricsContainer;

import java.util.List;


public interface PrometheusClient {
    String SUBSCRIPTION_QUERY_FORMAT = "sum by (group, topic, subscription)"
            + " (irate({__name__=~'%s', group='%s', topic='%s', subscription='%s', %s}[1m]))";

    String SUBSCRIPTION_QUERY_FORMAT_STATUS_CODE = "sum by (group, topic, subscription)"
            + " (irate({__name__=~'%s', group='%s', topic='%s', subscription='%s', status_code=~'%s', %s}[1m]))";

    String TOPIC_QUERY_FORMAT = "sum by (group, topic) (irate({__name__=~'%s', group='%s', "
            + "topic='%s', %s}[1m]))";

    default MonitoringMetricsContainer readMetrics(MetricsQuery... query) {
        return readMetrics(List.of(query));
    }

    MonitoringMetricsContainer readMetrics(List<MetricsQuery> queries);


    static MetricsQuery forSubscription(String name, SubscriptionName subscriptionName, String additionalFilters) {
        String fullQueryName = String.format(SUBSCRIPTION_QUERY_FORMAT, name,
                subscriptionName.getTopicName().getGroupName(), subscriptionName.getTopicName().getName(),
                subscriptionName.getName(), additionalFilters);
        return new MetricsQuery(fullQueryName);
    }

    static MetricsQuery forSubscriptionStatusCode(String name, SubscriptionName subscriptionName,
                                                  String regex, String additionalFilters) {
        String fullQuery = String.format(SUBSCRIPTION_QUERY_FORMAT_STATUS_CODE, name,
                subscriptionName.getTopicName().getGroupName(), subscriptionName.getTopicName().getName(),
                subscriptionName.getName(), regex, additionalFilters);
        return new MetricsQuery(fullQuery);
    }


    static MetricsQuery forTopic(String name, TopicName topicName, String additionalFilters) {
        String fullQuery = String.format(TOPIC_QUERY_FORMAT, name,
                topicName.getGroupName(), topicName.getName(), additionalFilters);
        return new MetricsQuery(fullQuery);
    }
}
