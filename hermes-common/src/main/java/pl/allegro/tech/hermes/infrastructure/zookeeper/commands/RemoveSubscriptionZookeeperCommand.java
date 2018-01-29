package pl.allegro.tech.hermes.infrastructure.zookeeper.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.infrastructure.zookeeper.SubscriptionPreconditions;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClient;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommand;

class RemoveSubscriptionZookeeperCommand extends ZookeeperCommand {
    private static final Logger logger = LoggerFactory.getLogger(RemoveSubscriptionZookeeperCommand.class);

    private final TopicName topicName;
    private final String subscriptionName;
    private final String subscriptionQualifiedName;
    private final SubscriptionPreconditions preconditions;

    private byte[] subscriptionDataBackup;

    RemoveSubscriptionZookeeperCommand(TopicName topicName, String subscriptionName, ZookeeperPaths paths) {
        super(paths);
        this.topicName = topicName;
        this.subscriptionName = subscriptionName;
        this.subscriptionQualifiedName = new SubscriptionName(subscriptionName, topicName).getQualifiedName();
        this.preconditions = new SubscriptionPreconditions(paths);
    }

    @Override
    public void backup(ZookeeperClient client) {
        preconditions.ensureSubscriptionExists(client, topicName, subscriptionName);

        subscriptionDataBackup = client.getData(getPath());
    }

    @Override
    public void execute(ZookeeperClient client) {
        preconditions.ensureSubscriptionExists(client, topicName, subscriptionName);

        logger.info("Removing subscription '{}' via client '{}'", subscriptionQualifiedName, client.getName());

        client.deleteWithChildrenWithGuarantee(getPath());
    }

    @Override
    public void rollback(ZookeeperClient client) {
        logger.info("Rolling back changes: subscription '{}' removal via client '{}'", subscriptionQualifiedName, client.getName());

        client.create(getPath(), subscriptionDataBackup);
    }

    private String getPath() {
        return paths.subscriptionPath(topicName, subscriptionName);
    }
}
