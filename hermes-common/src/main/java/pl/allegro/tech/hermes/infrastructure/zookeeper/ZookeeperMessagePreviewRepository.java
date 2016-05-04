package pl.allegro.tech.hermes.infrastructure.zookeeper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.domain.topic.preview.MessagePreviewRepository;
import pl.allegro.tech.hermes.domain.topic.preview.TopicsMessagesPreview;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

public class ZookeeperMessagePreviewRepository extends ZookeeperBasedRepository implements MessagePreviewRepository {

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperMessagePreviewRepository.class);

    public ZookeeperMessagePreviewRepository(CuratorFramework zookeeper, ObjectMapper mapper, ZookeeperPaths paths) {
        super(zookeeper, mapper, paths);
    }

    @Override
    public List<byte[]> loadPreview(TopicName topicName) {
        try {
            String path = paths.topicPath(topicName, ZookeeperPaths.PREVIEW_PATH);
            if (pathExists(path)) {
                return readFrom(path, new TypeReference<List<byte[]>>() {});
            } else {
                return new ArrayList<>();
            }
        } catch (Exception e) {
            throw new InternalProcessingException(
                    format("Could not read latest preview message for topic: %s.", topicName.qualifiedName()), e);
        }
    }

    @Override
    public void persist(TopicsMessagesPreview topicsMessagesPreview) {
        for (TopicName topic : topicsMessagesPreview.topics()) {
            persistMessage(topic, topicsMessagesPreview.previewOf(topic));
        }
    }

    private void persistMessage(TopicName topic, List<byte[]> messages) {
        logger.debug("Persisting {} messages for preview of topic: {}", messages.size(), topic.qualifiedName());
        try {
            if (pathExists(paths.topicPath(topic))) {
                String previewPath = paths.topicPath(topic, ZookeeperPaths.PREVIEW_PATH);
                ensurePathExists(previewPath);
                overwrite(previewPath, messages);
            }
        } catch (Exception exception) {
            logger.warn(
                    format("Could not log preview messages for topic: %s", topic.qualifiedName()),
                    exception
            );
        }
    }
}
