package pl.allegro.tech.hermes.common.message.undelivered;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.BackgroundPathAndBytesable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SentMessageTrace;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.String.format;
import static pl.allegro.tech.hermes.common.metric.Histograms.PERSISTED_UNDELIVERED_MESSAGE_SIZE;
import static pl.allegro.tech.hermes.common.metric.Meters.PERSISTED_UNDELIVERED_MESSAGES_METER;

public class ZookeeperUndeliveredMessageLog implements UndeliveredMessageLog {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperUndeliveredMessageLog.class);

    private final CuratorFramework curator;
    private final UndeliveredMessagePaths paths;
    private final ObjectMapper mapper;
    private final Meter persistedMessagesMeter;
    private final Histogram persistedMessageSizeHistogram;

    private final ConcurrentMap<SubscriptionName, SentMessageTrace> lastUndeliveredMessages = new ConcurrentHashMap<>();

    public ZookeeperUndeliveredMessageLog(CuratorFramework curator,
                                          ZookeeperPaths zookeeperPaths,
                                          ObjectMapper mapper,
                                          HermesMetrics metrics) {
        this.curator = curator;
        this.paths = new UndeliveredMessagePaths(zookeeperPaths);
        this.mapper = mapper;
        persistedMessagesMeter = metrics.meter(PERSISTED_UNDELIVERED_MESSAGES_METER);
        persistedMessageSizeHistogram = metrics.histogram(PERSISTED_UNDELIVERED_MESSAGE_SIZE);
    }

    @Override
    public void add(SentMessageTrace message) {
        lastUndeliveredMessages.put(new SubscriptionName(message.getSubscription(), message.getTopicName()), message);
    }

    @Override
    public void persist() {
        for (SubscriptionName key : lastUndeliveredMessages.keySet()) {
            log(lastUndeliveredMessages.remove(key));
        }
    }

    private void log(SentMessageTrace messageTrace) {
        try {
            String undeliveredPath = paths.buildPath(messageTrace.getTopicName(), messageTrace.getSubscription());
            BackgroundPathAndBytesable<?> builder = exists(undeliveredPath) ? curator.setData() : curator.create();
            byte[] bytesToPersist = mapper.writeValueAsBytes(messageTrace);
            builder.forPath(undeliveredPath, bytesToPersist);
            persistedMessagesMeter.mark();
            persistedMessageSizeHistogram.update(bytesToPersist.length);
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
