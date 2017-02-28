package pl.allegro.tech.hermes.consumers.supervisor.workload.selective;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.substringAfterLast;
import static org.apache.zookeeper.CreateMode.EPHEMERAL;

public class ConsumerNodesRegistry extends PathChildrenCache implements PathChildrenCacheListener {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerNodesRegistry.class);

    private final CuratorFramework curatorClient;
    private final String consumerNodeId;
    private final String prefix;
    private final LeaderLatch leaderLatch;
    private final Map<String, Long> consumersLastSeen = new HashMap<>();
    private final long deathOfConsumerAfterMillis;
    private final Clock clock;

    public ConsumerNodesRegistry(CuratorFramework curatorClient, ExecutorService executorService, String prefix,
                                 String consumerNodeId, int deathOfConsumerAfterSeconds, Clock clock) {
        super(curatorClient, getNodesPath(prefix), true, false, executorService);

        this.curatorClient = curatorClient;
        this.consumerNodeId = consumerNodeId;
        this.prefix = prefix;
        this.clock = clock;
        this.leaderLatch = new LeaderLatch(curatorClient, getLeaderPath(), consumerNodeId);
        this.deathOfConsumerAfterMillis = TimeUnit.SECONDS.toMillis(deathOfConsumerAfterSeconds);
    }

    @Override
    public void start() throws Exception {
        getListenable().addListener(this);
        super.start(StartMode.POST_INITIALIZED_EVENT);
    }

    @Override
    public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
        switch (event.getType()) {
            case INITIALIZED:
            case CONNECTION_RECONNECTED:
                if (!isRegistered(consumerNodeId)) {
                    registerConsumerNode();
                }
                break;
        }
    }

    public boolean isRegistered(String consumerNodeId) {
        try {
            return curatorClient.checkExists().forPath(getNodePath(consumerNodeId)) != null;
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    void startLeaderLatch() {
        try {
            leaderLatch.start();
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    void stopLeaderLatch() {
        try {
            leaderLatch.close();
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    boolean isLeader() {
        return ensureRegistered() && leaderLatch.hasLeadership();
    }

    List<String> list() {
        return new ArrayList<>(consumersLastSeen.keySet());
    }

    void refresh() {
        logger.info("Refreshing current consumers registry");

        long currentTime = clock.millis();
        readCurrentNodes().forEach(node -> consumersLastSeen.put(node, currentTime));

        List<String> deadConsumers = findDeadConsumers(currentTime);
        if (!deadConsumers.isEmpty()) {
            logger.info("Considering following consumers dead: {}", deadConsumers);
        }
        deadConsumers.forEach(consumersLastSeen::remove);
    }

    private boolean ensureRegistered() {
        if (curatorClient.getZookeeperClient().isConnected()) {
            if (!isRegistered(consumerNodeId)) {
                registerConsumerNode();
            }
            return true;
        }
        return false;
    }

    private void registerConsumerNode() {
        try {
            curatorClient.create().creatingParentsIfNeeded()
                    .withMode(EPHEMERAL).forPath(getNodePath(consumerNodeId));
            logger.info("Registered in consumer nodes registry as {}", consumerNodeId);
            refresh();
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    private String getNodePath(String consumerNodeId) {
        return getNodesPath(prefix) + "/" + consumerNodeId;
    }

    private static String getNodesPath(String prefix) {
        return prefix + "/nodes";
    }

    private String getLeaderPath() {
        return prefix + "/leader";
    }

    private List<String> findDeadConsumers(long currentTime) {
        long tooOld = currentTime - deathOfConsumerAfterMillis;
        return consumersLastSeen.entrySet().stream()
                .filter(entry -> {
                    long lastSeen = entry.getValue();
                    return lastSeen < tooOld;
                })
                .map(Map.Entry::getKey)
                .collect(toList());
    }

    private List<String> readCurrentNodes() {
        return getCurrentData().stream()
                .map(data -> substringAfterLast(data.getPath(), "/"))
                .collect(toList());
    }

    public String getId() {
        return consumerNodeId;
    }
}
