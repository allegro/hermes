package pl.allegro.tech.hermes.consumers.consumer.offset.kafka.broker;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalNotification;
import jersey.repackaged.com.google.common.collect.Lists;
import kafka.common.ErrorMapping;
import kafka.common.OffsetAndMetadata;
import kafka.common.OffsetMetadataAndError;
import kafka.common.TopicAndPartition;
import kafka.javaapi.OffsetCommitRequest;
import kafka.javaapi.OffsetCommitResponse;
import kafka.javaapi.OffsetFetchRequest;
import kafka.javaapi.OffsetFetchResponse;
import kafka.network.BlockingChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.kafka.ConsumerGroupId;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.util.HostnameResolver;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartition;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.broker.CannotCommitOffsetToBrokerException;

import javax.inject.Inject;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class BrokerOffsetsRepository {

    private static final Logger logger = LoggerFactory.getLogger(BrokerOffsetsRepository.class);

    /**
     * We need to increment offset by 1, because offset is inclusive for reading, meaning Kafka Consumer will pick up
     * reading from offset next time it starts. Without this increment, we would send committed message twice.
     */
    private static final int OFFSET_INCREMENT = 1;
    private static final String EMPTY_METADATA = "";
    private static final int CORRELATION_ID = 0;
    private static final short VERSION_ID = 1; // version 1 and above commit to Kafka, version 0 commits to ZooKeeper

    private final BlockingChannelFactory blockingChannelFactory;
    private final Clock clock;
    private final KafkaNamesMapper kafkaNamesMapper;

    private final LoadingCache<SubscriptionName, BlockingChannel> channels;
    private final String clientId;

    @Inject
    public BrokerOffsetsRepository(BlockingChannelFactory blockingChannelFactory, Clock clock, HostnameResolver hostnameResolver,
                                   ConfigFactory configFactory, KafkaNamesMapper kafkaNamesMapper) {
        this(blockingChannelFactory, clock, hostnameResolver, kafkaNamesMapper,
                configFactory.getIntProperty(Configs.KAFKA_CONSUMER_OFFSET_COMMITTER_BROKER_CONNECTION_EXPIRATION)
        );
    }

    public BrokerOffsetsRepository(BlockingChannelFactory blockingChannelFactory, Clock clock, HostnameResolver hostnameResolver,
                                   KafkaNamesMapper kafkaNamesMapper, int channelExpTime) {
        this.blockingChannelFactory = blockingChannelFactory;
        this.clock = clock;
        this.kafkaNamesMapper = kafkaNamesMapper;
        this.clientId = clientId(hostnameResolver);

        channels = CacheBuilder.newBuilder()
                .expireAfterAccess(channelExpTime, TimeUnit.SECONDS)
                .removalListener((RemovalNotification<SubscriptionName, BlockingChannel> notification) -> notification.getValue().disconnect())
                .build(new CacheLoader<SubscriptionName, BlockingChannel>() {
                    public BlockingChannel load(SubscriptionName key) {
                        BlockingChannel channel = blockingChannelFactory.create(kafkaNamesMapper.toConsumerGroupId(key));
                        channel.connect();
                        return channel;
                    }
                });
    }

    /**
     * Commit makes sure to save a proper commit "as a user would have wanted", taking into account
     * Kafka protocol. This means that saved offset needs to be incremented by one.
     *
     * @param subscriptionPartitionOffset information about subscription, partition & offset
     * @throws ExecutionException in case Kafka goes bad
     */
    public void commit(SubscriptionPartitionOffset subscriptionPartitionOffset) throws ExecutionException {
        save(new SubscriptionPartitionOffset(
                subscriptionPartitionOffset.getSubscriptionPartition(),
                subscriptionPartitionOffset.getOffset() + OFFSET_INCREMENT
        ));
    }

    /**
     * Saves exact passed offset, read commit method comments first.
     *
     * @param subscriptionPartitionOffset information about subscription, partition & offset
     * @throws ExecutionException in case Kafka goes bad
     */
    public void save(SubscriptionPartitionOffset subscriptionPartitionOffset) throws ExecutionException {
        SubscriptionName subscriptionName = subscriptionPartitionOffset.getSubscriptionName();
        OffsetCommitRequest commitRequest = createCommitRequest(subscriptionPartitionOffset);
        OffsetCommitResponse commitResponse;
        try {
            commitResponse = commitOffset(subscriptionName, commitRequest);
        } catch (Exception e) {
            channels.invalidate(subscriptionName);
            throw e;
        }
        if (commitResponse.hasError()) {
            commitResponse.errors().values()
                    .stream()
                    .map(error -> (Short) error)
                    .filter(error -> error == ErrorMapping.NotCoordinatorForConsumerCode() || error == ErrorMapping.ConsumerCoordinatorNotAvailableCode())
                    .findAny()
                    .ifPresent(error -> channels.invalidate(subscriptionName));

            throw new CannotCommitOffsetToBrokerException(new BrokerOffsetCommitErrors(commitResponse.errors()));
        }
    }

    private OffsetCommitRequest createCommitRequest(SubscriptionPartitionOffset subscriptionPartitionOffset) {
        Map<TopicAndPartition, OffsetAndMetadata> offset = createOffset(subscriptionPartitionOffset);

        return new OffsetCommitRequest(
                kafkaNamesMapper.toConsumerGroupId(subscriptionPartitionOffset.getSubscriptionName()).asString(),
                offset,
                CORRELATION_ID,
                clientId,
                VERSION_ID);
    }

    private Map<TopicAndPartition, OffsetAndMetadata> createOffset(SubscriptionPartitionOffset partitionOffset) {
        Map<TopicAndPartition, OffsetAndMetadata> offset = new LinkedHashMap<>();
        TopicAndPartition topicAndPartition = new TopicAndPartition(partitionOffset.getKafkaTopicName().asString(), partitionOffset.getPartition());
        offset.put(topicAndPartition, new OffsetAndMetadata(
                partitionOffset.getOffset(),
                EMPTY_METADATA,
                clock.millis())
        );
        return offset;
    }

    private OffsetCommitResponse commitOffset(SubscriptionName subscription, OffsetCommitRequest commitRequest) throws ExecutionException {
        BlockingChannel channel = channels.get(subscription);
        channel.send(commitRequest.underlying());
        return OffsetCommitResponse.readFrom(channel.receive().buffer());
    }

    public void saveIfOffsetInThePast(SubscriptionPartitionOffset subscriptionPartitionOffset) throws ExecutionException {
        long currentOffset = find(subscriptionPartitionOffset.getSubscriptionPartition());

        if (currentOffset == -1 || currentOffset > subscriptionPartitionOffset.getOffset()) {
            save(subscriptionPartitionOffset);
        } else {
            logger.warn("Tried to move offset for subscription {} and partition {} to {} which is in the future. Current offset: {}",
                    subscriptionPartitionOffset.getSubscriptionName(),
                    subscriptionPartitionOffset.getPartition(),
                    subscriptionPartitionOffset.getOffset(),
                    currentOffset);
        }
    }

    public long find(SubscriptionPartition subscriptionPartition) {
        ConsumerGroupId groupId = kafkaNamesMapper.toConsumerGroupId(subscriptionPartition.getSubscriptionName());
        BlockingChannel channel = blockingChannelFactory.create(groupId);
        channel.connect();

        TopicAndPartition topicAndPartition = new TopicAndPartition(
                subscriptionPartition.getKafkaTopicName().asString(), subscriptionPartition.getPartition()
        );
        List<TopicAndPartition> partitions = Lists.newArrayList(topicAndPartition);

        OffsetFetchRequest fetchRequest = new OffsetFetchRequest(
                groupId.asString(),
                partitions,
                VERSION_ID,
                CORRELATION_ID,
                clientId);

        channel.send(fetchRequest.underlying());
        OffsetFetchResponse fetchResponse = OffsetFetchResponse.readFrom(channel.receive().buffer());
        Map<TopicAndPartition, OffsetMetadataAndError> result = fetchResponse.offsets();
        OffsetMetadataAndError offset = result.get(topicAndPartition);
        channel.disconnect();
        return offset.offset();
    }

    private String clientId(HostnameResolver hostnameResolver) {
        return hostnameResolver.resolve() + "_" + UUID.randomUUID();
    }
}
