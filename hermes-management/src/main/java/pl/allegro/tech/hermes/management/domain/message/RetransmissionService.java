package pl.allegro.tech.hermes.management.domain.message;

import pl.allegro.tech.hermes.api.TopicName;

public interface RetransmissionService {

    void indicateOffsetChange(TopicName topic, String subscription, String brokersClusterName, long timestamp);

}
