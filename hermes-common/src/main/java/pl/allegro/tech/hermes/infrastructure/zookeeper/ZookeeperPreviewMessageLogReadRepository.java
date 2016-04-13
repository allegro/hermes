package pl.allegro.tech.hermes.infrastructure.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.domain.topic.preview.PreviewMessageLogReadRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;

public class ZookeeperPreviewMessageLogReadRepository extends ZookeeperBasedRepository implements PreviewMessageLogReadRepository {

    public ZookeeperPreviewMessageLogReadRepository(CuratorFramework zookeeper, ObjectMapper mapper, ZookeeperPaths paths) {
        super(zookeeper, mapper, paths);
    }

    @Override
    public List<byte[]> last(TopicName topicName) {
        try {
            String path = paths.topicPath(topicName, ZookeeperPaths.PREVIEW_PATH);

            if (exists(path)) {
                List<String> previewListPath = zookeeper.getChildren().forPath(path);
                List<byte[]> result = new ArrayList<>(previewListPath.size());
                for (String elementSubPath : previewListPath) {
                    result.add(zookeeper.getData().forPath(ZookeeperPaths.previewElementPath(path, elementSubPath)));
                }

                return result;
            } else {
                return Arrays.asList();
            }
        } catch (Exception e) {
            throw new InternalProcessingException(
                    format("Could not read latest preview message for topic: %s.",
                            topicName.qualifiedName()),
                    e);
        }
    }

    protected boolean exists(String path) throws Exception {
        return zookeeper.checkExists().forPath(path) != null;
    }
}
