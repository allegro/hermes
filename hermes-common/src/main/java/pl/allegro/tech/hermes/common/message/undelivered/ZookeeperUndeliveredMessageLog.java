package pl.allegro.tech.hermes.common.message.undelivered;

import static java.lang.String.format;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.BackgroundPathAndBytesable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SentMessageTrace;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.metrics.HermesCounter;
import pl.allegro.tech.hermes.metrics.HermesHistogram;

public class ZookeeperUndeliveredMessageLog implements UndeliveredMessageLog {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ZookeeperUndeliveredMessageLog.class);

  private final CuratorFramework curator;
  private final UndeliveredMessagePaths paths;
  private final ObjectMapper mapper;
  private final HermesCounter persistedMessagesMeter;
  private final HermesHistogram persistedMessageSizeHistogram;

  private final ConcurrentMap<SubscriptionName, SentMessageTrace> lastUndeliveredMessages =
      new ConcurrentHashMap<>();

  public ZookeeperUndeliveredMessageLog(
      CuratorFramework curator,
      ZookeeperPaths zookeeperPaths,
      ObjectMapper mapper,
      MetricsFacade metricsFacade) {
    this.curator = curator;
    this.paths = new UndeliveredMessagePaths(zookeeperPaths);
    this.mapper = mapper;
    persistedMessagesMeter = metricsFacade.undeliveredMessages().undeliveredMessagesCounter();
    persistedMessageSizeHistogram =
        metricsFacade.undeliveredMessages().undeliveredMessagesSizeHistogram();
  }

  @Override
  public void add(SentMessageTrace message) {
    lastUndeliveredMessages.put(
        new SubscriptionName(message.getSubscription(), message.getTopicName()), message);
  }

  @Override
  public void persist() {
    for (SubscriptionName key : lastUndeliveredMessages.keySet()) {
      log(lastUndeliveredMessages.remove(key));
    }
  }

  private void log(SentMessageTrace messageTrace) {
    try {
      String undeliveredPath =
          paths.buildPath(messageTrace.getTopicName(), messageTrace.getSubscription());
      BackgroundPathAndBytesable<?> builder =
          exists(undeliveredPath) ? curator.setData() : curator.create();
      byte[] bytesToPersist = mapper.writeValueAsBytes(messageTrace);
      builder.forPath(undeliveredPath, bytesToPersist);
      persistedMessagesMeter.increment();
      persistedMessageSizeHistogram.record(bytesToPersist.length);
    } catch (Exception exception) {
      LOGGER.warn(
          format(
              "Could not log undelivered message for topic: %s and subscription: %s",
              messageTrace.getQualifiedTopicName(), messageTrace.getSubscription()),
          exception);
    }
  }

  private boolean exists(String path) throws Exception {
    return curator.checkExists().forPath(path) != null;
  }
}
