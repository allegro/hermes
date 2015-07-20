package pl.allegro.tech.hermes.common.broker;

import kafka.common.OffsetAndMetadata;
import kafka.common.TopicAndPartition;
import kafka.javaapi.OffsetCommitRequest;
import kafka.javaapi.OffsetCommitResponse;
import kafka.network.BlockingChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.time.Clock;

import java.util.LinkedHashMap;
import java.util.Map;

public class KafkaOffsetsStorage implements OffsetsStorage {

    private static final Logger logger = LoggerFactory.getLogger(KafkaOffsetsStorage.class);
    private static final String EMPTY_METADATA = "";
    private static final String CLIENT_ID = "client";
    private static final int CORRELATION_ID = 0;
    private static final short VERSION = 1;

    private final BlockingChannelFactory blockingChannelFactory;
    private final Clock clock;

    public KafkaOffsetsStorage(BlockingChannelFactory blockingChannelFactory, Clock clock) {
        this.blockingChannelFactory = blockingChannelFactory;
        this.clock = clock;
    }

    @Override
    public void setSubscriptionOffset(TopicName topicName, String subscriptionName, int partitionId, long offset) {
        String groupId = Subscription.getId(topicName, subscriptionName);
        BlockingChannel channel = blockingChannelFactory.create(groupId);

        channel.connect();

        OffsetCommitRequest commitRequest = createCommitRequest(topicName, groupId, partitionId, offset);
        channel.send(commitRequest.underlying());
        OffsetCommitResponse commitResponse = OffsetCommitResponse.readFrom(channel.receive().buffer());

        if (commitResponse.hasError()) {
            logger.error("Cannot commit offset, error codes: %s ", new BrokerOffsetCommitErrors(commitResponse.errors()).toString());
        }

        channel.disconnect();
    }

    private OffsetCommitRequest createCommitRequest(TopicName topicName, String groupId, int partition, long offset) {
        Map<TopicAndPartition, OffsetAndMetadata> offsetMap = createOffset(topicName, partition, offset);

        return new OffsetCommitRequest(groupId, offsetMap, CORRELATION_ID, CLIENT_ID, VERSION);
    }

    private Map<TopicAndPartition, OffsetAndMetadata> createOffset(TopicName topicName, int partition, long offset) {
        Map<TopicAndPartition, OffsetAndMetadata> offsetMap = new LinkedHashMap<>();
        TopicAndPartition topicAndPartition = new TopicAndPartition(topicName.qualifiedName(), partition);
        offsetMap.put(topicAndPartition, new OffsetAndMetadata(offset, EMPTY_METADATA, clock.getTime()));
        return offsetMap;
    }
}
