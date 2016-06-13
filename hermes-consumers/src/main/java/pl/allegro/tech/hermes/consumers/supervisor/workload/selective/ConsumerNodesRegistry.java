package pl.allegro.tech.hermes.consumers.supervisor.workload.selective;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.substringAfterLast;
import static org.apache.zookeeper.CreateMode.EPHEMERAL;

public class ConsumerNodesRegistry extends PathChildrenCache implements PathChildrenCacheListener {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerNodesRegistry.class);

    private final CuratorFramework curatorClient;
    private final String consumerNodeId;
    private final String prefix;
    private final LeaderLatch leaderLatch;

    public ConsumerNodesRegistry(CuratorFramework curatorClient, ExecutorService executorService, String prefix, String consumerNodeId) {
        super(curatorClient, getNodesPath(prefix), true, false, executorService);

        this.curatorClient = curatorClient;
        this.consumerNodeId = consumerNodeId;
        this.prefix = prefix;
        this.leaderLatch = new LeaderLatch(curatorClient, getLeaderPath(), consumerNodeId);
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

    private void registerConsumerNode() {
        try {
            curatorClient.create().creatingParentsIfNeeded()
                    .withMode(EPHEMERAL).forPath(getNodePath(consumerNodeId));
            logger.info("Registered in consumer nodes registry as {}", consumerNodeId);
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    public void registerLeaderLatchListener(LeaderLatchListener... leaderListener) {
        try {
            leaderLatch.start();
            Arrays.stream(leaderListener).forEach(leaderLatch::addListener);
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

    public boolean isLeader() {
        return curatorClient.getZookeeperClient().isConnected() && leaderLatch.hasLeadership();
    }

    public List<String> list() {
        return getCurrentData().stream().map(data -> substringAfterLast(data.getPath(), "/")).collect(toList());
    }

    public String getId() {
        return consumerNodeId;
    }
}
