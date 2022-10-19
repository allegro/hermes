package pl.allegro.tech.hermes.common.message.undelivered;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.BackgroundPathAndBytesable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SentMessageTrace;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.String.format;

public class ZookeeperUndeliveredMessageLog implements UndeliveredMessageLog {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperUndeliveredMessageLog.class);

    private static final String NODE_NAME = "undelivered";

    private final CuratorFramework curator;
    private final ZookeeperPaths paths;
    private final ObjectMapper mapper;

    private final ConcurrentMap<SubscriptionName, SentMessageTrace> lastUndeliveredMessages = new ConcurrentHashMap<>();

    public ZookeeperUndeliveredMessageLog(CuratorFramework curator, ZookeeperPaths zookeeperPaths, ObjectMapper mapper) {
        this.curator = curator;
        this.paths = zookeeperPaths;
        this.mapper = mapper;
    }

    @Override
    public void add(SentMessageTrace message) {
        lastUndeliveredMessages.put(new SubscriptionName(message.getSubscription(), message.getTopicName()), message);
    }

    @Override
    public Optional<SentMessageTrace> last(TopicName topicName, String subscriptionName) {
        try {
            String path = paths.subscriptionPath(topicName, subscriptionName, NODE_NAME);

            if (exists(path)) {
                return Optional.of(mapper.readValue(curator.getData().forPath(path), SentMessageTrace.class));
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            throw new InternalProcessingException(
                    format("Could not read latest undelivered message for topic: %s and subscription: %s .",
                            topicName.qualifiedName(), subscriptionName),
                    e);
        }
    }

    @Override
    public void persist() {
        for (SubscriptionName key : lastUndeliveredMessages.keySet()) {
            log(lastUndeliveredMessages.remove(key));
        }
    }

    private void log(SentMessageTrace messageTrace) {
        try {
            String undeliveredPath = paths.subscriptionPath(messageTrace.getTopicName(), messageTrace.getSubscription(), NODE_NAME);
            BackgroundPathAndBytesable<?> builder = exists(undeliveredPath) ? curator.setData() : curator.create();
            builder.forPath(undeliveredPath, mapper.writeValueAsBytes(messageTrace));
        } catch (Exception exception) {
            LOGGER.warn(
                    format("Could not log undelivered message for topic: %s and subscription: %s",
                            messageTrace.getQualifiedTopicName(),
                            messageTrace.getSubscription()
                    ),
                    exception
            );
        }
    }

    private boolean exists(String path) throws Exception {
        return curator.checkExists().forPath(path) != null;
    }

}
