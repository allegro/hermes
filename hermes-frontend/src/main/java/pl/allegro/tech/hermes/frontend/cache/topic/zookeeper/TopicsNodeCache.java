package pl.allegro.tech.hermes.frontend.cache.topic.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.cache.zookeeper.StartableCache;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicCallback;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

class TopicsNodeCache extends StartableCache<TopicCallback> implements PathChildrenCacheListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TopicsNodeCache.class);

    private final ObjectMapper objectMapper;

    private Map<String, Topic> topicMap = new ConcurrentHashMap<>();

    public TopicsNodeCache(CuratorFramework client, ObjectMapper objectMapper, String path, ExecutorService executorService) {
        super(client, path, executorService);
        this.objectMapper = objectMapper;
        getListenable().addListener(this);
    }

    @Override
    public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
        if (event.getData() == null || event.getData().getData() == null) {
            LOGGER.warn("Unrecognized event {}", event);
            return;
        }
        String path = event.getData().getPath();
        Topic topic = readTopic(event);
        LOGGER.info("Got topic change event for path {} type {}", path, event.getType());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Event data {}", new String(event.getData().getData(), Charsets.UTF_8));
        }
        switch (event.getType()) {
            case CHILD_ADDED:
                for (TopicCallback callback : callbacks) {
                    callback.onTopicCreated(topic);
                }
                topicMap.put(topic.getName().getName(), topic);
                break;
            case CHILD_REMOVED:
                for (TopicCallback callback : callbacks) {
                    callback.onTopicRemoved(topic);
                }
                topicMap.remove(topic.getName().getName());
                break;
            case CHILD_UPDATED:
                for (TopicCallback callback : callbacks) {
                    callback.onTopicChanged(topic);
                }
                topicMap.put(topic.getName().getName(), topic);
                break;
            default:
                break;
        }
    }

    public Topic getTopic(String name) {
        return topicMap.get(name);
    }

    private Topic readTopic(PathChildrenCacheEvent event) throws IOException {
        return objectMapper.readValue(event.getData().getData(), Topic.class);
    }

}
