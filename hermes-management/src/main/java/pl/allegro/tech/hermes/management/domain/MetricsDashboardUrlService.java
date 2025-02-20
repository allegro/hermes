package pl.allegro.tech.hermes.management.domain;

public interface MetricsDashboardUrlService {

  String getUrlForTopic(String topic);

  String getUrlForSubscription(String topic, String subscription);
}
