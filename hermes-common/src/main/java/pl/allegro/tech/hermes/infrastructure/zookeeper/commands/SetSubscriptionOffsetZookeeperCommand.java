package pl.allegro.tech.hermes.infrastructure.zookeeper.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.infrastructure.zookeeper.SubscriptionPreconditions;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClient;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommand;

import java.nio.charset.StandardCharsets;

class SetSubscriptionOffsetZookeeperCommand extends ZookeeperCommand {
    private static final Logger logger = LoggerFactory.getLogger(SetSubscriptionOffsetZookeeperCommand.class);

    private final SetSubscriptionOffsetData input;
    private final String subscriptionQualifiedName;
    private final SubscriptionPreconditions preconditions;

    private boolean offsetPathExisted;
    private byte[] offsetBackup;

    SetSubscriptionOffsetZookeeperCommand(SetSubscriptionOffsetData input, ZookeeperPaths paths) {
        super(paths);
        this.input = input;
        this.subscriptionQualifiedName = new SubscriptionName(input.getSubscriptionName(),
                input.getTopicName()).getQualifiedName();
        this.preconditions = new SubscriptionPreconditions(paths);
    }

    @Override
    public void backup(ZookeeperClient client) {
        preconditions.ensureSubscriptionExists(client, input.getTopicName(), input.getSubscriptionName());

        String offsetPath = getOffsetPath();
        offsetPathExisted = client.pathExists(offsetPath);
        if(offsetPathExisted) {
            offsetBackup = client.getData(offsetPath);
        }
    }

    @Override
    public void execute(ZookeeperClient client) {
        preconditions.ensureSubscriptionExists(client, input.getTopicName(), input.getSubscriptionName());

        logger.info("Changing subscription {} offset via client {}", subscriptionQualifiedName, client.getName());

        String offsetPath = getOffsetPath();
        byte[] offset = getOffsetAsBytes();
        client.upsert(offsetPath, offset);
    }

    @Override
    public void rollback(ZookeeperClient client) {
        logger.info("Rolling back changes: subscription {} offset change via client {}", subscriptionQualifiedName,
                client.getName());

        String offsetPath = getOffsetPath();
        if(offsetPathExisted) {
            client.setData(getOffsetPath(), offsetBackup);
        } else {
            client.delete(offsetPath);
        }
    }

    private String getOffsetPath() {
        return paths.offsetPath(input.getTopicName(), input.getSubscriptionName(),
                input.getPartitionOffset().getTopic(), input.getBrokersClusterName(),
                input.getPartitionOffset().getPartition());
    }

    private byte[] getOffsetAsBytes() {
        return String.valueOf(input.getPartitionOffset().getOffset()).getBytes(StandardCharsets.UTF_8);
    }
}
