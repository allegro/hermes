package pl.allegro.tech.hermes.frontend.producer.kafka;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;

class TopicMinInSyncReplicasResolver {
    private final int minInSyncReplicasAckLeader;
    private final int minInSyncReplicasAckAll;

    TopicMinInSyncReplicasResolver(int minInSyncReplicasAckLeader, int minInSyncReplicasAckAll) {
        this.minInSyncReplicasAckLeader = minInSyncReplicasAckLeader;
        this.minInSyncReplicasAckAll = minInSyncReplicasAckAll;
    }

    public Integer resolveMinNumberReplicaForTopic(CachedTopic cachedTopic) {
        return Topic.Ack.ALL == cachedTopic.getTopic().getAck() ? minInSyncReplicasAckAll : minInSyncReplicasAckLeader;
    }
}
