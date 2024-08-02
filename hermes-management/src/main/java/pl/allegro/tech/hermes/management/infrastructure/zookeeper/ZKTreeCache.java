package pl.allegro.tech.hermes.management.infrastructure.zookeeper;

import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.group.GroupNotExistsException;
import pl.allegro.tech.hermes.domain.notifications.GroupCallback;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;
import pl.allegro.tech.hermes.domain.notifications.SubscriptionCallback;
import pl.allegro.tech.hermes.domain.notifications.TopicCallback;
import pl.allegro.tech.hermes.domain.topic.TopicNotExistsException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;


public class ZKTreeCache {
    private static final Logger logger = LoggerFactory.getLogger(ZKTreeCache.class);

    private final InternalNotificationsBus notificationsBus;
    private final Tree tree = new Tree();

    public ZKTreeCache(InternalNotificationsBus notificationsBus) {
        this.notificationsBus = notificationsBus;
        notificationsBus.registerGroupCallback(tree);
        notificationsBus.registerTopicCallback(tree);
        notificationsBus.registerSubscriptionCallback(tree);
    }

    public List<Group> getGroups() {
        return tree.getGroups();
    }

    public List<String> listGroupNames() {
        return getGroups().stream().map(Group::getGroupName).collect(Collectors.toList());
    }

    public List<Subscription> listSubscriptions(TopicName topicName) {
        return tree.getSubscriptions(topicName);
    }

    public List<Topic> listTopics(String groupName) {
        return tree.getTopics(groupName);
    }

    private static class Tree implements GroupCallback, TopicCallback, SubscriptionCallback {
        private final ReadWriteLock lock = new ReentrantReadWriteLock();
        private final RootNode root = new RootNode();

        static class RootNode {
            private final Map<String, GroupNode> groups = new HashMap<>();

            void putGroup(String groupName, GroupNode groupNode) {
                this.groups.put(groupName, groupNode);
            }

            GroupNode getGroup(String groupName) {
                return this.groups.get(groupName);
            }

            void removeGroup(String groupName) {
                this.groups.remove(groupName);
            }
        }

        static class GroupNode {
            private final Group group;
            private final Map<String, TopicNode> topics;

            public GroupNode(Group group, Map<String, TopicNode> topics) {
                this.topics = topics;
                this.group = group;
            }

            @Nullable
            public TopicNode getTopic(String topicName) {
                return topics.get(topicName);
            }

            public void putTopic(String topicName, TopicNode topicNode) {
                topics.put(topicName, topicNode);
            }

            public void removeTopic(String topicName) {
                topics.remove(topicName);
            }

            public List<Topic> getTopics() {
                return topics.values().stream().map(t -> t.topic).collect(Collectors.toList());
            }
        }

        static class TopicNode {
            private final Topic topic;
            private final Map<String, SubscriptionNode> subscriptions;

            public TopicNode(Topic topic, Map<String, SubscriptionNode> subscriptions) {
                this.topic = topic;
                this.subscriptions = subscriptions;
            }

            public void putSubscription(String subscriptionName, SubscriptionNode subscriptionNode) {
                this.subscriptions.put(subscriptionName, subscriptionNode);
            }

            public void removeSubscription(String subscriptionName) {
                this.subscriptions.remove(subscriptionName);
            }

            public List<Subscription> getSubscriptions() {
                return subscriptions.values().stream().map(s -> s.subscription).collect(Collectors.toList());
            }
        }

        record SubscriptionNode(Subscription subscription) {
        }

        void withWriteLock(Runnable runnable) {
            Lock writeLock = lock.writeLock();
            writeLock.lock();
            try {
                runnable.run();
            } finally {
                writeLock.unlock();
            }

        }

        <T> T withReadLock(Supplier<T> supplier) {
            Lock readLock = lock.writeLock();
            readLock.lock();
            T value;
            try {
                value = supplier.get();
            } finally {
                readLock.unlock();
            }
            return value;
        }

        public List<Group> getGroups() {
            return withReadLock(() -> root.groups.values().stream().map(g -> g.group).collect(Collectors.toList()));
        }

        public List<Topic> getTopics(String groupName) {
            return withReadLock(() -> {
                Tree.GroupNode parent = root.getGroup(groupName);
                if (parent == null) {
                    throw new GroupNotExistsException(groupName);
                }
                ;
                return parent.getTopics();
            });
        }

        public List<Subscription> getSubscriptions(TopicName topicName) {
            return withReadLock(() -> {
                Tree.GroupNode groupNode = root.getGroup(topicName.getGroupName());
                if (groupNode == null) {
                    throw new TopicNotExistsException(topicName);
                }

                Tree.TopicNode parent = groupNode.getTopic(topicName.getName());
                if (parent == null) {
                    throw new TopicNotExistsException(topicName);
                }
                return parent.getSubscriptions();
            });
        }


        public void onGroupChanged(Group group) {
            logger.debug("onGroupChanged: {}", group);
            withWriteLock(() -> {
                GroupNode previous = root.getGroup(group.getGroupName());

                Map<String, TopicNode> topics = previous == null ?
                        new HashMap<>() : previous.topics;
                GroupNode groupNode = new GroupNode(group, topics);
                root.putGroup(group.getGroupName(), groupNode);
            });
        }

        public void onGroupCreated(Group group) {
            onGroupChanged(group);
        }

        public void onGroupRemoved(Group group) {
            logger.debug("onGroupRemoved: {}", group);
            withWriteLock(() -> {
                root.removeGroup(group.getGroupName());
            });
        }

        public void onTopicChanged(Topic topic) {
            logger.debug("onTopicChanged: {}", topic);
            withWriteLock(() -> {
                GroupNode parent = root.getGroup(topic.getName().getGroupName());
                if (parent == null) {
                    logger.debug("Received onTopicChanged: {}, but group not found in tree. ignoring", topic);
                    return;
                }
                TopicNode previous = parent.getTopic(topic.getName().getName());

                Map<String, SubscriptionNode> subscriptions = previous == null ?
                        new HashMap<>() : previous.subscriptions;

                TopicNode topicNode = new TopicNode(topic, subscriptions);
                parent.putTopic(topic.getName().getName(), topicNode);
            });

        }

        public void onTopicCreated(Topic topic) {
            onTopicChanged(topic);
        }

        public void onTopicRemoved(Topic topic) {
            logger.debug("onTopicRemoved: {}", topic);
            withWriteLock(() -> {
                GroupNode parent = root.getGroup(topic.getName().getGroupName());
                if (parent == null) {
                    logger.debug("onTopicRemoved: {} but group not found in tree. ignoring", topic);
                    return;
                }
                parent.removeTopic(topic.getName().getName());
            });

        }

        public void onSubscriptionChanged(Subscription subscription) {
            logger.debug("onSubscriptionChanged: {}", subscription);
            withWriteLock(() -> {
                GroupNode groupNode = root.groups.get(subscription.getTopicName().getGroupName());
                if (groupNode == null) {
                    logger.debug("onSubscriptionChanged: {} but group not found in tree. ignoring", subscription);
                    return;
                }

                TopicNode parent = groupNode.topics.get(subscription.getTopicName().getName());
                if (parent == null) {
                    logger.debug("onSubscriptionChanged: {} but topic not found in tree. ignoring", subscription);
                    return;
                }
                SubscriptionNode subscriptionNode = new SubscriptionNode(subscription);
                parent.putSubscription(subscription.getName(), subscriptionNode);
            });

        }

        public void onSubscriptionCreated(Subscription subscription) {
            onSubscriptionChanged(subscription);
        }

        public void onSubscriptionRemoved(Subscription subscription) {
            logger.debug("onSubscriptionRemoved: {}", subscription);
            withWriteLock(() -> {
                GroupNode groupNode = root.groups.get(subscription.getTopicName().getGroupName());

                if (groupNode == null) {
                    logger.debug("onSubscriptionRemoved: {} but group not found in tree. ignoring", subscription);
                    return;
                }

                TopicNode parent = groupNode.topics.get(subscription.getTopicName().getName());
                if (parent == null) {
                    logger.debug("onSubscriptionRemoved: {} but topic not found in tree. ignoring", subscription);
                    return;
                }
                parent.removeSubscription(subscription.getName());
            });
        }
    }
}
