package pl.allegro.tech.hermes.infrastructure.zookeeper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.domain.topic.preview.MessagePreview;
import pl.allegro.tech.hermes.domain.topic.preview.MessagePreviewRepository;
import pl.allegro.tech.hermes.domain.topic.preview.TopicsMessagesPreview;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClient;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClientManager;

import java.util.Collections;
import java.util.List;

import static java.lang.String.format;

public class DistributedZookeeperMessagePreviewRepository extends DistributedZookeeperRepository
        implements MessagePreviewRepository{
    private static final Logger logger = LoggerFactory.getLogger(DistributedZookeeperMessagePreviewRepository.class);

    private final ZookeeperPaths paths;

    public DistributedZookeeperMessagePreviewRepository(ZookeeperClientManager clientManager, ZookeeperPaths paths,
                                                 ObjectMapper mapper) {
        super(clientManager, mapper);
        this.paths = paths;
    }

    @Override
    public List<MessagePreview> loadPreview(TopicName topicName) {
        for(ZookeeperClient client : clientManager.getClients()) { //TODO can be parallel
            List<MessagePreview> preview = loadPreviewFromClient(client, topicName);
            if(!preview.isEmpty()) {
                return preview;
            }
        }
        return Collections.emptyList();
    }

    private List<MessagePreview> loadPreviewFromClient(ZookeeperClient client, TopicName topicName) {
        String previewPath = paths.topicPath(topicName, ZookeeperPaths.PREVIEW_PATH);
        try {
            if(client.pathExists(previewPath)) {
                byte[] data = client.getData(previewPath);
                return unmarshall(data, new TypeReference<List<MessagePreview>>() {});
            } else {
                return Collections.emptyList();
            }
        } catch (Exception e) {
            throw new InternalProcessingException(
                    format("Could not read latest preview message for topic: %s.", topicName.qualifiedName()), e);
        }
    }

    @Override
    public void persist(TopicsMessagesPreview topicsMessagesPreview) {
        ZookeeperClient client = clientManager.getLocalClient();

        for (TopicName topic : topicsMessagesPreview.topics()) {
            persistMessage(client, topic, topicsMessagesPreview.previewOf(topic));
        }
    }

    private void persistMessage(ZookeeperClient client, TopicName topic, List<MessagePreview> messages) {
        logger.info("Persisting {} messages for preview of topic {} via client {}", messages.size(),
                topic.qualifiedName(), client.getName());

        try {
            String topicPath = paths.topicPath(topic);
            if (client.pathExists(topicPath)) {
                String previewPath = paths.topicPath(topic, ZookeeperPaths.PREVIEW_PATH);
                client.upsert(previewPath, marshall(messages));
            }
        } catch (Exception exception) {
            logger.warn(
                    format("Could not log preview messages for topic: %s", topic.qualifiedName()),
                    exception
            );
        }
    }
}
