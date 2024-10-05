package pl.allegro.tech.hermes.infrastructure.zookeeper;

import static java.lang.String.format;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.domain.topic.preview.MessagePreview;
import pl.allegro.tech.hermes.domain.topic.preview.MessagePreviewRepository;
import pl.allegro.tech.hermes.domain.topic.preview.TopicsMessagesPreview;

public class ZookeeperMessagePreviewRepository extends ZookeeperBasedRepository
    implements MessagePreviewRepository {

  private static final Logger logger =
      LoggerFactory.getLogger(ZookeeperMessagePreviewRepository.class);

  public ZookeeperMessagePreviewRepository(
      CuratorFramework zookeeper, ObjectMapper mapper, ZookeeperPaths paths) {
    super(zookeeper, mapper, paths);
  }

  @Override
  public List<MessagePreview> loadPreview(TopicName topicName) {
    try {
      return Optional.of(paths.topicPreviewPath(topicName))
          .filter(this::pathExists)
          .flatMap(p -> readFrom(p, new TypeReference<List<MessagePreview>>() {}, true))
          .orElseGet(ArrayList::new);
    } catch (Exception e) {
      throw new InternalProcessingException(
          format("Could not read latest preview message for topic: %s.", topicName.qualifiedName()),
          e);
    }
  }

  @Override
  public void persist(TopicsMessagesPreview topicsMessagesPreview) {
    for (TopicName topic : topicsMessagesPreview.topics()) {
      persistMessage(topic, topicsMessagesPreview.previewOf(topic));
    }
  }

  private void persistMessage(TopicName topic, List<MessagePreview> messages) {
    logger.debug(
        "Persisting {} messages for preview of topic: {}", messages.size(), topic.qualifiedName());
    try {
      if (pathExists(paths.topicPath(topic))) {
        String previewPath = paths.topicPreviewPath(topic);
        ensurePathExists(previewPath);
        overwrite(previewPath, messages);
      }
    } catch (Exception exception) {
      logger.warn(
          format("Could not log preview messages for topic: %s", topic.qualifiedName()), exception);
    }
  }
}
