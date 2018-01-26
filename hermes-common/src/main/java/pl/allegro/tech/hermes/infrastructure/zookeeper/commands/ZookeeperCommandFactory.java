package pl.allegro.tech.hermes.infrastructure.zookeeper.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.OAuthProvider;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.topic.preview.TopicsMessagesPreview;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommand;

public class ZookeeperCommandFactory {

    private ZookeeperPaths paths;
    private ObjectMapper mapper;

    public ZookeeperCommandFactory(ZookeeperPaths paths, ObjectMapper mapper) {
        this.paths = paths;
        this.mapper = mapper;
    }

    public CreateGroupZookeeperCommand createGroup(Group group) {
        return new CreateGroupZookeeperCommand(group, paths, mapper);
    }

    public UpdateGroupZookeeperCommand updateGroup(Group group) {
        return new UpdateGroupZookeeperCommand(group, paths, mapper);
    }

    public RemoveGroupZookeeperCommand removeGroup(String groupName) {
        return new RemoveGroupZookeeperCommand(groupName, paths);
    }

    public CreateTopicZookeeperCommand createTopic(Topic topic) {
        return new CreateTopicZookeeperCommand(topic, paths, mapper);
    }

    public RemoveTopicZookeeperCommand removeTopic(TopicName topicName) {
        return new RemoveTopicZookeeperCommand(topicName, paths);
    }

    public UpdateTopicZookeeperCommand updateTopic(Topic topic) {
        return new UpdateTopicZookeeperCommand(topic, paths, mapper);
    }

    public TouchTopicZookeeperCommand touchTopic(TopicName topicName) {
        return new TouchTopicZookeeperCommand(topicName, paths);
    }

    public CreateSubscriptionZookeeperCommand createSubscription(Subscription subscription) {
        return new CreateSubscriptionZookeeperCommand(subscription, paths, mapper);
    }

    public RemoveSubscriptionZookeeperCommand removeSubscription(TopicName topicName, String subscriptionName) {
        return new RemoveSubscriptionZookeeperCommand(topicName, subscriptionName, paths);
    }

    public UpdateSubscriptionZookeeperCommand updateSubscription(Subscription subscription) {
        return new UpdateSubscriptionZookeeperCommand(subscription, paths, mapper);
    }

    public CreateOAuthProviderZookeeperCommand createOAuthProvider(OAuthProvider provider) {
        return new CreateOAuthProviderZookeeperCommand(provider, paths, mapper);
    }

    public RemoveOAuthProviderZookeeperCommand removeOAuthProvider(String providerName) {
        return new RemoveOAuthProviderZookeeperCommand(providerName, paths);
    }

    public UpdateOAuthProviderZookeeperCommand updateOAuthProvider(OAuthProvider provider) {
        return new UpdateOAuthProviderZookeeperCommand(provider, paths, mapper);
    }

    public SetSubscriptionOffsetZookeeperCommand setSubscriptionOffset(SetSubscriptionOffsetData data) {
        return new SetSubscriptionOffsetZookeeperCommand(data, paths);
    }
}
