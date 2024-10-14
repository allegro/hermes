package pl.allegro.tech.hermes.management.infrastructure.prometheus;

import java.util.List;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringMetricsContainer;

public interface PrometheusClient {
  String SUBSCRIPTION_QUERY_FORMAT =
      "sum by (group, topic, subscription)"
          + " (irate({__name__='%s', group='%s', topic='%s', subscription='%s', %s}[1m]))";

  String SUBSCRIPTION_QUERY_FORMAT_STATUS_CODE =
      "sum by (group, topic, subscription)"
          + " (irate({__name__='%s', group='%s', topic='%s', subscription='%s', status_code=~'%s', %s}[1m]))";

  String TOPIC_QUERY_FORMAT =
      "sum by (group, topic) (irate({__name__='%s', group='%s', " + "topic='%s', %s}[1m]))";

  default MonitoringMetricsContainer readMetrics(String... query) {
    return readMetrics(List.of(query));
  }

  MonitoringMetricsContainer readMetrics(List<String> queries);

  static String forSubscription(
      String name, SubscriptionName subscriptionName, String additionalFilters) {
    return String.format(
        SUBSCRIPTION_QUERY_FORMAT,
        name,
        subscriptionName.getTopicName().getGroupName(),
        subscriptionName.getTopicName().getName(),
        subscriptionName.getName(),
        additionalFilters);
  }

  static String forSubscriptionStatusCode(
      String name, SubscriptionName subscriptionName, String regex, String additionalFilters) {
    return String.format(
        SUBSCRIPTION_QUERY_FORMAT_STATUS_CODE,
        name,
        subscriptionName.getTopicName().getGroupName(),
        subscriptionName.getTopicName().getName(),
        subscriptionName.getName(),
        regex,
        additionalFilters);
  }

  static String forTopic(String name, TopicName topicName, String additionalFilters) {
    return String.format(
        TOPIC_QUERY_FORMAT, name, topicName.getGroupName(), topicName.getName(), additionalFilters);
  }
}
