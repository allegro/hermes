package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.broker;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalNotification;
import kafka.common.ErrorMapping;
import kafka.common.OffsetAndMetadata;
import kafka.common.TopicAndPartition;
import kafka.javaapi.OffsetCommitRequest;
import kafka.javaapi.OffsetCommitResponse;
import kafka.network.BlockingChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.broker.BlockingChannelFactory;
import pl.allegro.tech.hermes.common.broker.BrokerOffsetCommitErrors;
import pl.allegro.tech.hermes.common.time.Clock;
import pl.allegro.tech.hermes.common.util.HostnameResolver;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageCommitter;
import pl.allegro.tech.hermes.domain.subscription.offset.PartitionOffset;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class BrokerMessageCommitter implements MessageCommitter {

    private static final Logger logger = LoggerFactory.getLogger(BrokerMessageCommitter.class);

    private static final String EMPTY_METADATA = "";
    private static final int CORRELATION_ID = 0;
    private static final short VERSION_ID = 1; // version 1 and above commit to Kafka, version 0 commits to ZooKeeper

    private final Clock clock;
    private final String clientId;

    private final LoadingCache<Subscription, BlockingChannel> channels;

    public BrokerMessageCommitter(BlockingChannelFactory blockingChannelFactory,
                                  Clock clock,
                                  HostnameResolver hostnameResolver,
                                  int channelExpTime) {
        this.clock = clock;
        this.clientId = clientId(hostnameResolver);

        channels = CacheBuilder.newBuilder()
                .expireAfterAccess(channelExpTime, TimeUnit.SECONDS)
                .removalListener((RemovalNotification<Subscription, BlockingChannel> notification) -> notification.getValue().disconnect())
                .build(new CacheLoader<Subscription, BlockingChannel>() {
                    public BlockingChannel load(Subscription key) {
                        BlockingChannel channel = blockingChannelFactory.create(key.getId());
                        channel.connect();
                        return channel;
                    }
                });
    }

    @Override
    public void commitOffset(Subscription subscription, PartitionOffset partitionOffset) throws Exception {
        OffsetCommitRequest commitRequest = createCommitRequest(subscription, partitionOffset);
        OffsetCommitResponse commitResponse = commitOffset(subscription, commitRequest);

        if (commitResponse.hasError()) {
            commitResponse.errors().values()
                    .stream()
                    .map(e -> Short.parseShort(e.toString()))
                    .filter(e -> (e == ErrorMapping.NotCoordinatorForConsumerCode() || e == ErrorMapping.NotCoordinatorForConsumerCode()))
                    .forEach(e -> channels.invalidate(subscription));

            throw new CannotCommitOffsetToBrokerException(new BrokerOffsetCommitErrors(commitResponse.errors()));
        }
    }

    private String clientId(HostnameResolver hostnameResolver) {
        return hostnameResolver.resolve() + "_" + UUID.randomUUID();
    }

    private OffsetCommitResponse commitOffset(Subscription subscription, OffsetCommitRequest commitRequest) throws java.util.concurrent.ExecutionException {
        BlockingChannel channel = channels.get(subscription);
        channel.send(commitRequest.underlying());
        return OffsetCommitResponse.readFrom(channel.receive().buffer());
    }

    private OffsetCommitRequest createCommitRequest(Subscription subscription, PartitionOffset partitionOffset) {
        Map<TopicAndPartition, OffsetAndMetadata> offset = createOffset(subscription, partitionOffset);

        return new OffsetCommitRequest(
                subscription.getId(),
                offset,
                CORRELATION_ID,
                clientId,
                VERSION_ID);
    }

    private Map<TopicAndPartition, OffsetAndMetadata> createOffset(Subscription subscription, PartitionOffset partitionOffset) {
        Map<TopicAndPartition, OffsetAndMetadata> offset = new LinkedHashMap<>();
        TopicAndPartition topicAndPartition = new TopicAndPartition(subscription.getTopicName().qualifiedName(), partitionOffset.getPartition());
        offset.put(topicAndPartition, new OffsetAndMetadata(partitionOffset.getOffset(), EMPTY_METADATA, clock.getTime()));
        return offset;
    }

    @Override
    public void removeOffset(TopicName topicName, String subscriptionName, int partition) throws Exception {
        //Consumers commit their offsets in Kafka by writing them to topic - so offsets will be removed after specified retention time.
    }
}
