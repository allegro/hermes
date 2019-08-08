package pl.allegro.tech.hermes.consumers.supervisor.workload.constraints;

import pl.allegro.tech.hermes.api.TopicName;

public class TopicConstraints {

    private final TopicName topicName;
    private final int consumersNumber;

    public TopicConstraints(TopicName topicName, int consumersNumber) {
        this.topicName = topicName;
        this.consumersNumber = consumersNumber;
    }

    public TopicName getTopicName() {
        return topicName;
    }

    public int getConsumersNumber() {
        return consumersNumber;
    }
}
