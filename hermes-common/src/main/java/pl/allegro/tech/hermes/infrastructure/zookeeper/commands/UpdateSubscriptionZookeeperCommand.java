package pl.allegro.tech.hermes.infrastructure.zookeeper.commands;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.infrastructure.zookeeper.SubscriptionPreconditions;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClient;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommand;

class UpdateSubscriptionZookeeperCommand extends ZookeeperCommand {
    private static final Logger logger = LoggerFactory.getLogger(UpdateSubscriptionZookeeperCommand.class);

    private final Subscription subscription;
    private final SubscriptionPreconditions preconditions;
    private final ObjectMapper mapper;

    private byte[] subscriptionDataBackup;

    UpdateSubscriptionZookeeperCommand(Subscription subscription, ZookeeperPaths paths, ObjectMapper mapper) {
        super(paths);
        this.subscription = subscription;
        this.preconditions = new SubscriptionPreconditions(paths);
        this.mapper = mapper;
    }

    @Override
    public void backup(ZookeeperClient client) {
        preconditions.ensureSubscriptionExists(client, subscription.getTopicName(), subscription.getName());

        subscriptionDataBackup = client.getData(getPath());
    }

    @Override
    public void execute(ZookeeperClient client) {
        preconditions.ensureSubscriptionExists(client, subscription.getTopicName(), subscription.getName());

        logger.info("Updating subscription {} via client {}", subscription.getQualifiedName(), client.getName());
        client.setData(getPath(), marshall(mapper, subscription));
    }

    @Override
    public void rollback(ZookeeperClient client) {
        logger.info("Rolling back changes: subscription {} update via client {}", subscription.getQualifiedName(), client.getName());

        client.setData(getPath(), subscriptionDataBackup);
    }

    private String getPath() {
        return paths.subscriptionPath(subscription);
    }
}
