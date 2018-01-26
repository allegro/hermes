package pl.allegro.tech.hermes.infrastructure.zookeeper.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionAlreadyExistsException;
import pl.allegro.tech.hermes.infrastructure.zookeeper.TopicPreconditions;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClient;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommand;

class CreateSubscriptionZookeeperCommand extends ZookeeperCommand {
    private static final Logger logger = LoggerFactory.getLogger(CreateSubscriptionZookeeperCommand.class);

    private final Subscription subscription;
    private final TopicPreconditions preconditions;
    private final ObjectMapper mapper;

    CreateSubscriptionZookeeperCommand(Subscription subscription, ZookeeperPaths paths, ObjectMapper mapper) {
        super(paths);
        this.subscription = subscription;
        this.preconditions = new TopicPreconditions(paths);
        this.mapper = mapper;
    }

    @Override
    public void backup(ZookeeperClient client) {
    }

    @Override
    public void execute(ZookeeperClient client) {
        preconditions.ensureTopicExists(client, subscription.getTopicName());

        logger.info("Creating subscription {} via client {}", subscription.getQualifiedName(), client.getName());
        String path = paths.subscriptionPath(subscription);

        try {
            client.getCuratorFramework().create().forPath(path, mapper.writeValueAsBytes(subscription));
        } catch (KeeperException.NodeExistsException ex) {
            throw new SubscriptionAlreadyExistsException(subscription, ex);
        } catch (Exception ex) {
            throw new InternalProcessingException(ex);
        }
    }

    @Override
    public void rollback(ZookeeperClient client) {
        String path = paths.subscriptionPath(subscription);
        client.deleteWithChildren(path);
    }
}
