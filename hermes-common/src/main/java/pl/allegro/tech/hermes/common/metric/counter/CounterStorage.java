package pl.allegro.tech.hermes.common.metric.counter;

import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.metric.Metrics;

public interface CounterStorage {

    void setTopicCounter(TopicName topicName, Metrics.Counter counter, long count);

    void setSubscriptionCounter(TopicName topicName, String subscriptionName, Metrics.Counter counter, long count);

    void setInflightCounter(String hostname, TopicName topicName, String subscriptionName, long count);

    long getTopicCounter(TopicName topicName, Metrics.Counter counter);

    long getSubscriptionCounter(TopicName topicName, String subscriptionName, Metrics.Counter counter);

    long getInflightCounter(TopicName topicName, String subscriptionName);

    int countInflightNodes(TopicName topicName, String subscriptionName);
}
