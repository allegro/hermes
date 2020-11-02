package pl.allegro.tech.hermes.management.domain.consistency

import groovy.transform.AutoImplement
import pl.allegro.tech.hermes.api.Group
import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.api.Topic
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.domain.group.GroupNotExistsException
import pl.allegro.tech.hermes.domain.group.GroupRepository
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository
import pl.allegro.tech.hermes.domain.topic.TopicRepository
import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder
import pl.allegro.tech.hermes.management.domain.dc.RepositoryManager

class MockRepositoryManager implements RepositoryManager {
    private final Map<String, DatacenterDefinition> datacenterDefinitionMap = new HashMap<>()

    @Override
    <T> DatacenterBoundRepositoryHolder<T> getLocalRepository(Class<T> repositoryType) {
        throw new UnsupportedOperationException()
    }

    @Override
    <T> List<DatacenterBoundRepositoryHolder<T>> getRepositories(Class<T> repositoryType) {
        switch (repositoryType) {
            case GroupRepository:
                return datacenterDefinitionMap.values().collect { it.toGroupRepositoryHolder() }
            case TopicRepository:
                return datacenterDefinitionMap.values().collect { it.toTopicRepositoryHolder() }
            case SubscriptionRepository:
                return datacenterDefinitionMap.values().collect { it.toSubscriptionRepositoryHolder() }
        }
        throw new UnsupportedOperationException()
    }

    DatacenterDefinition datacenter(String datacenter) {
        DatacenterDefinition definition = datacenterDefinitionMap.getOrDefault(datacenter, new DatacenterDefinition(datacenter))
        datacenterDefinitionMap.put(datacenter, definition)
        return definition
    }

    void reset() {
        datacenterDefinitionMap.clear()
    }

    class DatacenterDefinition {
        private final String datacenter
        private final Map<String, Group> groups = new HashMap<>()
        private final Map<String, Topic> topics = new HashMap<>()
        private final Map<String, Subscription> subscriptions = new HashMap<>()

        private DatacenterDefinition(String datacenter) {
            this.datacenter = datacenter
        }

        DatacenterDefinition addGroup(Group group) {
            groups.put(group.getGroupName(), group)
            return this
        }

        DatacenterDefinition addTopic(Topic topic) {
            topics.put(topic.getQualifiedName(), topic)
            return this
        }

        DatacenterDefinition addSubscription(Subscription subscription) {
            subscriptions.put(subscription.getQualifiedName().getQualifiedName(), subscription)
            return this
        }

        private DatacenterBoundRepositoryHolder<GroupRepository> toGroupRepositoryHolder() {
            return new DatacenterBoundRepositoryHolder<GroupRepository>(new MockGroupRepository(groups), datacenter)
        }

        private DatacenterBoundRepositoryHolder<TopicRepository> toTopicRepositoryHolder() {
            return new DatacenterBoundRepositoryHolder<TopicRepository>(new MockTopicRepository(topics), datacenter)
        }

        private DatacenterBoundRepositoryHolder<SubscriptionRepository> toSubscriptionRepositoryHolder() {
            return new DatacenterBoundRepositoryHolder<SubscriptionRepository>(new MockSubscriptionRepository(subscriptions), datacenter)
        }
    }

    @AutoImplement(exception = UnsupportedOperationException, message = 'Not supported by MockGroupRepository')
    class MockGroupRepository implements GroupRepository {
        private final Map<String, Group> groups

        private MockGroupRepository(Map<String, Group> groups) {
            this.groups = groups
        }

        @Override
        Group getGroupDetails(String groupName) {
            Group group = groups.get(groupName)
            if (group == null) {
                throw new GroupNotExistsException(groupName)
            }
            return group
        }

        @Override
        List<String> listGroupNames() {
            return new ArrayList<String>(groups.keySet())
        }
    }

    @AutoImplement(exception = UnsupportedOperationException, message = 'Not supported by MockTopicRepository')
    class MockTopicRepository implements TopicRepository {
        private final Map<String, Topic> topics

        private MockTopicRepository(Map<String, Topic> topics) {
            this.topics = topics
        }

        @Override
        List<Topic> listTopics(String groupName) {
            return topics.values().findAll { it -> (it.getName().getGroupName() == groupName) }
        }
    }

    @AutoImplement(exception = UnsupportedOperationException, message = 'Not supported by MockSubscriptionRepository')
    class MockSubscriptionRepository implements SubscriptionRepository {
        private final Map<String, Subscription> subscriptions

        private MockSubscriptionRepository(Map<String, Subscription> subscriptions) {
            this.subscriptions = subscriptions
        }

        @Override
        List<Subscription> listSubscriptions(TopicName topicName) {
            return subscriptions.values().findAll { it -> (it.getTopicName() == topicName) }
        }
    }
}
