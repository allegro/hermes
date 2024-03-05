package pl.allegro.tech.hermes.frontend.producer;

import pl.allegro.tech.hermes.frontend.metric.CachedTopic;

public interface BrokerTopicMetadataFetcher {

    boolean tryFetchFromLocalDatacenter(CachedTopic topic);

    boolean tryFetchFromDatacenter(CachedTopic topic, String datacenter);

    void close();
}
