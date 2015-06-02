package pl.allegro.tech.hermes.infrastructure.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionAlreadyExistsException;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionNotExistsException;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;

import java.util.List;
import java.util.stream.Collectors;

public class ZookeeperSubscriptionRepository extends ZookeeperBasedRepository implements SubscriptionRepository {

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperSubscriptionRepository.class);

    private final TopicRepository topicRepository;

    public ZookeeperSubscriptionRepository(CuratorFramework zookeeper,
                                           ObjectMapper mapper,
                                           ZookeeperPaths paths,
                                           TopicRepository topicRepository) {
        super(zookeeper, mapper, paths);
        this.topicRepository = topicRepository;
    }

    @Override
    public boolean subscriptionExists(TopicName topicName, String subscriptionName) {
        return pathExists(paths.subscriptionPath(topicName, subscriptionName));
    }

    @Override
    public void ensureSubscriptionExists(TopicName topicName, String subscriptionName) {
        if (!subscriptionExists(topicName, subscriptionName)) {
            throw new SubscriptionNotExistsException(topicName, subscriptionName);
        }
    }

    @Override
    public void createSubscription(Subscription subscription) {
        ensureConnected();
        topicRepository.ensureTopicExists(subscription.getTopicName());

        String subscriptionPath = paths.subscriptionPath(subscription);
        logger.info("Creating subscription for path {}", subscriptionPath);

        try {
            zookeeper.create().forPath(subscriptionPath, mapper.writeValueAsBytes(subscription));
        } catch (KeeperException.NodeExistsException ex) {
            throw new SubscriptionAlreadyExistsException(subscription, ex);
        } catch (Exception ex) {
            throw new InternalProcessingException(ex);
        }
    }

    @Override
    public void removeSubscription(TopicName topicName, String subscriptionName) {
        ensureSubscriptionExists(topicName, subscriptionName);
        logger.info("Removing subscription {} for topic {}", subscriptionName, topicName.qualifiedName());

        remove(paths.subscriptionPath(topicName, subscriptionName));
    }

    @Override
    public void updateSubscription(Subscription modifiedSubscription) {
        ensureSubscriptionExists(modifiedSubscription.getTopicName(), modifiedSubscription.getName());
        logger.info("Updating subscription {} for topic {}",
                modifiedSubscription.getName(),
                modifiedSubscription.getTopicName().qualifiedName()
        );

        overwrite(paths.subscriptionPath(modifiedSubscription), modifiedSubscription);
    }

    @Override
    public void updateSubscriptionState(TopicName topicName, String subscriptionName, Subscription.State state) {
        ensureSubscriptionExists(topicName, subscriptionName);

        logger.info("Changing subscription {} for topic {} state to {}", subscriptionName, topicName, state.toString());

        Subscription modifiedSubscription = getSubscriptionDetails(topicName, subscriptionName);
        if (!modifiedSubscription.getState().equals(state)) {
            modifiedSubscription.setState(state);
            updateSubscription(modifiedSubscription);
        }
    }

    @Override
    public Subscription getSubscriptionDetails(TopicName topicName, String subscriptionName) {
        ensureSubscriptionExists(topicName, subscriptionName);
        return readFrom(paths.subscriptionPath(topicName, subscriptionName), Subscription.class);
    }

    @Override
    public List<String> listSubscriptionNames(TopicName topicName) {
        return childrenOf(paths.subscriptionsPath(topicName));
    }

    @Override
    public List<Subscription> listSubscriptions(TopicName topicName) {
        return listSubscriptionNames(topicName).stream()
                .map(subscription -> getSubscriptionDetails(topicName, subscription))
                .collect(Collectors.toList());
    }

    @Override
    public List<String> listTrackedSubscriptionNames(TopicName topicName) {
        return listSubscriptions(topicName).stream()
                .filter(Subscription::isTrackingEnabled)
                .map(Subscription::getName)
                .collect(Collectors.toList());
    }
}
