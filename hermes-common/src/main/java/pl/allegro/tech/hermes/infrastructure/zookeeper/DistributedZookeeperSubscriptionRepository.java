package pl.allegro.tech.hermes.infrastructure.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClient;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClientManager;
import pl.allegro.tech.hermes.infrastructure.zookeeper.commands.ZookeeperCommandFactory;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommand;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommandExecutor;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommandFailedException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DistributedZookeeperSubscriptionRepository extends DistributedZookeeperRepository
        implements SubscriptionRepository {
    private static final Logger logger = LoggerFactory.getLogger(DistributedZookeeperSubscriptionRepository.class);

    private final ZookeeperCommandExecutor commandExecutor;
    private final ZookeeperCommandFactory commandFactory;
    private final ZookeeperPaths paths;
    private final SubscriptionPreconditions preconditions;

    public DistributedZookeeperSubscriptionRepository(ZookeeperClientManager clientManager,
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
    public boolean subscriptionExists(TopicName topicName, String subscriptionName) {
        return getClient().pathExists(paths.subscriptionPath(topicName, subscriptionName));
    }

    @Override
    public void ensureSubscriptionExists(TopicName topicName, String subscriptionName) {
        preconditions.ensureSubscriptionExists(getClient(), topicName, subscriptionName);
    }

    @Override
    public void createSubscription(Subscription subscription) {
        ZookeeperCommand command = commandFactory.createSubscription(subscription);
        try {
            commandExecutor.execute(command);
        } catch (ZookeeperCommandFailedException e) {
            throw new InternalProcessingException(e);
        }
    }

    @Override
    public void removeSubscription(TopicName topicName, String subscriptionName) {
        ZookeeperCommand command = commandFactory.removeSubscription(topicName, subscriptionName);
        try {
            commandExecutor.execute(command);
        } catch (ZookeeperCommandFailedException e) {
            throw new InternalProcessingException(e);
        }
    }

    @Override
    public void updateSubscription(Subscription modifiedSubscription) {
        ZookeeperCommand command = commandFactory.updateSubscription(modifiedSubscription);
        try {
            commandExecutor.execute(command);
        } catch (ZookeeperCommandFailedException e) {
            throw new InternalProcessingException(e);
        }
    }

    @Override
    public void updateSubscriptionState(TopicName topicName, String subscriptionName, Subscription.State state) {
        ZookeeperClient client = getClient();

        preconditions.ensureSubscriptionExists(client, topicName, subscriptionName);

        Subscription modifiedSubscription = getSubscriptionDetails(topicName, subscriptionName);
        if (!modifiedSubscription.getState().equals(state)) {
            logger.info("Changing subscription {} state to {}",
                    new SubscriptionName(subscriptionName, topicName).getQualifiedName(), state.toString());

            modifiedSubscription.setState(state);

            ZookeeperCommand command = commandFactory.updateSubscription(modifiedSubscription);
            try {
                commandExecutor.execute(command);
            } catch (ZookeeperCommandFailedException e) {
                throw new InternalProcessingException(e);
            }
        }
    }

    @Override
    public Subscription getSubscriptionDetails(TopicName topicName, String subscriptionName) {
        ZookeeperClient client = getClient();
        return getSubscriptionDetails(client, topicName, subscriptionName, false).get();
    }

    @Override
    public Subscription getSubscriptionDetails(SubscriptionName name) {
        return getSubscriptionDetails(name.getTopicName(), name.getName());
    }

    @Override
    public List<String> listSubscriptionNames(TopicName topicName) {
        ZookeeperClient client = getClient();
        return listSubscriptionNames(client, topicName);
    }

    private List<String> listSubscriptionNames(ZookeeperClient client, TopicName topicName) {
        String path = paths.subscriptionsPath(topicName);
        return client.childrenOf(path);
    }

    @Override
    public List<Subscription> listSubscriptions(TopicName topicName) {
        ZookeeperClient client = getClient();

        return listSubscriptionNames(topicName).stream()
                .map(subscription -> getSubscriptionDetails(client, topicName, subscription, true))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private Optional<Subscription> getSubscriptionDetails(ZookeeperClient client, TopicName topicName, String subscriptionName, boolean quiet) {
        preconditions.ensureSubscriptionExists(client, topicName, subscriptionName);

        String path = paths.subscriptionPath(topicName, subscriptionName);
        return client.readFrom(path, (data) -> mapper.readValue(data, Subscription.class), quiet);
    }

    private ZookeeperClient getClient() {
        ZookeeperClient client = clientManager.getLocalClient();
        client.ensureConnected();
        return client;
    }
}
