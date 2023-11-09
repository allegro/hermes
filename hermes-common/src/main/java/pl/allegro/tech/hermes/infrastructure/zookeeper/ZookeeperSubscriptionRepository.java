package pl.allegro.tech.hermes.infrastructure.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionAlreadyExistsException;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionNotExistsException;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
        topicRepository.ensureTopicExists(subscription.getTopicName());

        String subscriptionPath = paths.subscriptionPath(subscription);
        logger.info("Creating subscription {}", subscription.getQualifiedName());

        try {
            create(subscriptionPath, subscription);
        } catch (KeeperException.NodeExistsException ex) {
            throw new SubscriptionAlreadyExistsException(subscription, ex);
        } catch (Exception ex) {
            throw new InternalProcessingException(ex);
        }
    }

    @Override
    public void removeSubscription(TopicName topicName, String subscriptionName) {
        ensureSubscriptionExists(topicName, subscriptionName);
        logger.info("Removing subscription {}", new SubscriptionName(subscriptionName, topicName).getQualifiedName());

        try {
            remove(paths.subscriptionPath(topicName, subscriptionName));
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    @Override
    public void updateSubscription(Subscription modifiedSubscription) {
        ensureSubscriptionExists(modifiedSubscription.getTopicName(), modifiedSubscription.getName());
        logger.info("Updating subscription {}", modifiedSubscription.getQualifiedName());
        try {
            overwrite(paths.subscriptionPath(modifiedSubscription), modifiedSubscription);
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    @Override
    public void updateSubscriptionState(TopicName topicName, String subscriptionName, Subscription.State state) {
        ensureSubscriptionExists(topicName, subscriptionName);

        logger.info("Changing subscription {} state to {}",
                new SubscriptionName(subscriptionName, topicName).getQualifiedName(), state.toString());

        Subscription modifiedSubscription = getSubscriptionDetails(topicName, subscriptionName);
        if (!modifiedSubscription.getState().equals(state)) {
            modifiedSubscription.setState(state);
            updateSubscription(modifiedSubscription);
        }
    }

    @Override
    public Subscription getSubscriptionDetails(TopicName topicName, String subscriptionName) {
        ensureSubscriptionExists(topicName, subscriptionName);
        return readWithStatFrom(
                paths.subscriptionPath(topicName, subscriptionName),
                Subscription.class,
                (sub, stat) -> {
                    sub.setCreatedAt(stat.getCtime());
                    sub.setModifiedAt(stat.getMtime());
                },
                false
        ).get();
    }

    private Optional<Subscription> getSubscriptionDetails(TopicName topicName, String subscriptionName, boolean quiet) {
        ensureSubscriptionExists(topicName, subscriptionName);
        return readFrom(paths.subscriptionPath(topicName, subscriptionName), Subscription.class, quiet);
    }

    @Override
    public Subscription getSubscriptionDetails(SubscriptionName name) {
        return getSubscriptionDetails(name.getTopicName(), name.getName());
    }

    @Override
    public List<Subscription> getSubscriptionDetails(Collection<SubscriptionName> subscriptionNames) {
        return subscriptionNames.stream()
                .map(n -> getSubscriptionDetails(n.getTopicName(), n.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<String> listSubscriptionNames(TopicName topicName) {
        topicRepository.ensureTopicExists(topicName);

        return childrenOf(paths.subscriptionsPath(topicName));
    }

    @Override
    public List<Subscription> listSubscriptions(TopicName topicName) {
        return listSubscriptionNames(topicName).stream()
                .map(subscription -> getSubscriptionDetails(topicName, subscription, true))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public List<Subscription> listAllSubscriptions() {
        return topicRepository.listAllTopics().stream()
                .map(topic -> listSubscriptions(topic.getName()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }
}
