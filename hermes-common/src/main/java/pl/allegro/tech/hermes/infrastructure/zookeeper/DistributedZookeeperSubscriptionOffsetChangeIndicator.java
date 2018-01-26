package pl.allegro.tech.hermes.infrastructure.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffsets;
import pl.allegro.tech.hermes.common.kafka.offset.SubscriptionOffsetChangeIndicator;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClient;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClientManager;
import pl.allegro.tech.hermes.infrastructure.zookeeper.commands.SetSubscriptionOffsetData;
import pl.allegro.tech.hermes.infrastructure.zookeeper.commands.ZookeeperCommandFactory;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommand;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommandExecutor;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommandFailedException;

import java.nio.charset.StandardCharsets;

public class DistributedZookeeperSubscriptionOffsetChangeIndicator extends DistributedZookeeperRepository
        implements SubscriptionOffsetChangeIndicator {

    private final ZookeeperCommandExecutor commandExecutor;
    private final ZookeeperCommandFactory commandFactory;
    private final ZookeeperPaths paths;
    private final SubscriptionPreconditions preconditions;

    public DistributedZookeeperSubscriptionOffsetChangeIndicator(ZookeeperClientManager clientManager,
                                                                 ZookeeperCommandExecutor commandExecutor,
                                                                 ZookeeperCommandFactory commandFactory,
                                                                 ZookeeperPaths paths,
                                                                 ObjectMapper mapper) {
        super(clientManager, mapper);
        this.commandExecutor = commandExecutor;
        this.commandFactory = commandFactory;
        this.paths = paths;
        this.preconditions = new SubscriptionPreconditions(paths);
    }

    @Override
    public void setSubscriptionOffset(TopicName topicName, String subscriptionName, String brokersClusterName,
                                      PartitionOffset partitionOffset) {
        SetSubscriptionOffsetData data = new SetSubscriptionOffsetData(
                topicName,
                subscriptionName,
                brokersClusterName,
                partitionOffset
        );
        ZookeeperCommand command = commandFactory.setSubscriptionOffset(data);
        try {
            commandExecutor.execute(command);
        } catch (ZookeeperCommandFailedException e) {
            throw new InternalProcessingException(e);
        }
    }

    @Override
    public PartitionOffsets getSubscriptionOffsets(TopicName topic, String subscriptionName,
                                                   String brokersClusterName) {
        ZookeeperClient client = clientManager.getLocalClient();

        preconditions.ensureSubscriptionExists(client, topic, subscriptionName);

        PartitionOffsets allOffsets = new PartitionOffsets();

        String kafkaTopicsPath = paths.subscribedKafkaTopicsPath(topic, subscriptionName);
        client.childrenOf(kafkaTopicsPath)
                .stream()
                .map(KafkaTopicName::valueOf)
                .forEach(kafkaTopic -> {
                    PartitionOffsets offsets = getOffsetsForKafkaTopic(client, topic, kafkaTopic, subscriptionName,
                            brokersClusterName);
                    allOffsets.addAll(offsets);
                });
        return allOffsets;
    }

    private PartitionOffsets getOffsetsForKafkaTopic(ZookeeperClient client, TopicName topic,
                                                     KafkaTopicName kafkaTopicName, String subscriptionName,
                                                     String brokersClusterName) {
        String offsetsPath = paths.offsetsPath(topic, subscriptionName, kafkaTopicName, brokersClusterName);

        PartitionOffsets offsets = new PartitionOffsets();
        client.childrenOf(offsetsPath)
                .stream()
                .map(Integer::valueOf)
                .map(partition -> {
                    Long partitionOffset = getOffsetForPartition(client, topic, kafkaTopicName, subscriptionName,
                            brokersClusterName, partition);
                    return new PartitionOffset(kafkaTopicName, partitionOffset, partition);
                })
                .forEach(offsets::add);

        return offsets;
    }

    private Long getOffsetForPartition(ZookeeperClient client, TopicName topic, KafkaTopicName kafkaTopicName,
                                       String subscriptionName, String brokersClusterName, int partitionId) {
        String offsetPath = paths.offsetPath(topic, subscriptionName, kafkaTopicName, brokersClusterName, partitionId);
        try {
            byte[] data = client.getData(offsetPath);
            return Long.valueOf(new String(data, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }
}
