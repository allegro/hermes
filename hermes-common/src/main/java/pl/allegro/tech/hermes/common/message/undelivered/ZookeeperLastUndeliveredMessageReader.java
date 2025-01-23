package pl.allegro.tech.hermes.common.message.undelivered;

import static java.lang.String.format;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.api.SentMessageTrace;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

public class ZookeeperLastUndeliveredMessageReader implements LastUndeliveredMessageReader {

  private final CuratorFramework curator;
  private final UndeliveredMessagePaths paths;
  private final ObjectMapper mapper;

  public ZookeeperLastUndeliveredMessageReader(
      CuratorFramework curator, ZookeeperPaths zookeeperPaths, ObjectMapper mapper) {
    this.curator = curator;
    this.paths = new UndeliveredMessagePaths(zookeeperPaths);
    this.mapper = mapper;
  }

  @Override
  public Optional<SentMessageTrace> last(TopicName topicName, String subscriptionName) {
    try {
      String path = paths.buildPath(topicName, subscriptionName);
      if (exists(path)) {
        return Optional.of(
            mapper.readValue(curator.getData().forPath(path), SentMessageTrace.class));
      } else {
        return Optional.empty();
      }
    } catch (Exception e) {
      throw new InternalProcessingException(
          format(
              "Could not read latest undelivered message for topic: %s and subscription: %s .",
              topicName.qualifiedName(), subscriptionName),
          e);
    }
  }

  private boolean exists(String path) throws Exception {
    return curator.checkExists().forPath(path) != null;
  }
}
