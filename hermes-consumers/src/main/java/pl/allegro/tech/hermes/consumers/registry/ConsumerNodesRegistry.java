package pl.allegro.tech.hermes.consumers.registry;

import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

import java.io.IOException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.apache.zookeeper.CreateMode.EPHEMERAL;

public class ConsumerNodesRegistry extends PathChildrenCache implements PathChildrenCacheListener {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerNodesRegistry.class);

    private final CuratorFramework curatorClient;
    private final ConsumerNodesRegistryPaths registryPaths;
    private final String consumerNodeId;
    private final LeaderLatch leaderLatch;
    private final Map<String, Long> consumersLastSeen = new ConcurrentHashMap<>();
    private final long deathOfConsumerAfterMillis;
    private final Clock clock;

    public ConsumerNodesRegistry(
            CuratorFramework curatorClient,
            ExecutorService executorService,
            ConsumerNodesRegistryPaths registryPaths,
            String consumerNodeId,
            long deathOfConsumerAfterSeconds,
            Clock clock
    ) {
        super(curatorClient, registryPaths.nodesPath(), true, false, executorService);

        this.curatorClient = curatorClient;
        this.registryPaths = registryPaths;
        this.consumerNodeId = consumerNodeId;
        this.clock = clock;
        this.leaderLatch = new LeaderLatch(curatorClient, registryPaths.leaderPath(), consumerNodeId);
        this.deathOfConsumerAfterMillis = TimeUnit.SECONDS.toMillis(deathOfConsumerAfterSeconds);
    }

    @Override
    public void start() throws Exception {
        getListenable().addListener(this);
        super.start(StartMode.POST_INITIALIZED_EVENT);
        leaderLatch.start();
    }

    public void stop() throws IOException {
        leaderLatch.close();
        close();
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
            default:
                // noop
                break;
        }
    }

    public boolean isRegistered(String consumerNodeId) {
        try {
            return curatorClient.checkExists().forPath(registryPaths.nodePath(consumerNodeId)) != null;
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    public boolean isLeader() {
        return ensureRegistered() && leaderLatch.hasLeadership();
    }

    public List<String> listConsumerNodes() {
        return new ArrayList<>(consumersLastSeen.keySet());
    }

    public synchronized void refresh() {
        logger.info("Refreshing current consumers registry");

        long currentTime = clock.millis();
        List<String> currentNodes = readCurrentNodes();
        List<String> validNodes = currentNodes.stream()
                .filter(StringUtils::isNotBlank)
                .toList();
        if (currentNodes.size() != validNodes.size()) {
            logger.warn("Found {} invalid consumer nodes.", currentNodes.size() - validNodes.size());
        }
        validNodes.forEach(node -> consumersLastSeen.put(node, currentTime));

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
            String nodePath = registryPaths.nodePath(consumerNodeId);
            if (curatorClient.checkExists().forPath(nodePath) == null) {
                curatorClient.create().creatingParentsIfNeeded()
                        .withMode(EPHEMERAL).forPath(nodePath);
                logger.info("Registered in consumer nodes registry as {}", consumerNodeId);
            }
        } catch (NodeExistsException e) {
            // Ignore as it is a race condition between threads trying to register the consumer node.
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
        refresh();
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

    public String getConsumerId() {
        return consumerNodeId;
    }

    public void addLeaderLatchListener(LeaderLatchListener listener) {
        leaderLatch.addListener(listener);
    }

    public void removeLeaderLatchListener(LeaderLatchListener listener) {
        leaderLatch.removeListener(listener);
    }
}
