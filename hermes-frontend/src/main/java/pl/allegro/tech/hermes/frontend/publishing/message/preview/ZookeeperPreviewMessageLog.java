package pl.allegro.tech.hermes.frontend.publishing.message.preview;

import com.google.common.collect.EvictingQueue;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.BackgroundPathAndBytesable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPreviewMessageLogReadRepository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.String.format;

public class ZookeeperPreviewMessageLog extends ZookeeperPreviewMessageLogReadRepository implements PreviewMessageLog {

    public static final int DEFAULT_PREVIEW_SIZE = 2;

    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperPreviewMessageLog.class);

    private final ConcurrentMap<TopicName, EvictingQueue<PreviewMessage>> samples = new ConcurrentHashMap<>();

    public ZookeeperPreviewMessageLog(CuratorFramework zookeeper, ZookeeperPaths zookeeperPaths) {
        super(zookeeper, null, zookeeperPaths);
    }

    @Override
    public void add(byte[] messageContent, TopicName topicName) {
        EvictingQueue<PreviewMessage> messages = samples.get(topicName);
        if (messages == null) {
            messages = EvictingQueue.create(DEFAULT_PREVIEW_SIZE);
            samples.put(topicName, messages);
        }
        messages.add(new PreviewMessage(messageContent));
    }

    @Override
    public void persist() {
        for (Map.Entry<TopicName, EvictingQueue<PreviewMessage>> pairs : samples.entrySet()) {
            logTopic(pairs.getKey(), pairs.getValue().toArray(new PreviewMessage[0]));
        }
    }

    private void logTopic(TopicName topicName, PreviewMessage[] messages) {
        LOGGER.info("Presist for topic:{} samples #{}", topicName.qualifiedName(), messages.length);
        try {
            String previewPath = paths.topicPath(topicName, ZookeeperPaths.PREVIEW_PATH);

            for (int i = 0; i < messages.length; i++) {
                String elementPath = ZookeeperPaths.previewElementPath(previewPath, String.valueOf(i));
                BackgroundPathAndBytesable<?> builder = exists(elementPath) ? zookeeper.setData() : zookeeper.create().creatingParentsIfNeeded();
                builder.forPath(elementPath, messages[i].getData());
            }
        } catch (Exception exception) {
            LOGGER.warn(
                    format("Could not log preview messages for topic: %s",
                            topicName.qualifiedName()
                    ),
                    exception
            );
        }
    }

    static class PreviewMessage {
        private final byte[] data;

        public PreviewMessage(byte[] data) {
            this.data = data;
        }

        public byte[] getData() {
            return data;
        }
    }

}
