package pl.allegro.tech.hermes.infrastructure.zookeeper.counter;

import com.google.common.primitives.Longs;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DistributedEphemeralCounter {

    private static final Logger logger = LoggerFactory.getLogger(DistributedEphemeralCounter.class);

    private final CuratorFramework curatorClient;

    public DistributedEphemeralCounter(CuratorFramework curatorClient) {
        this.curatorClient = curatorClient;
    }

    public void setCounterValue(String path, long count) {
        try {
            ensureCounterExists(path);
            curatorClient.setData().forPath(path, Longs.toByteArray(count));
        } catch (Exception e) {
            throw new ZookeeperCounterException(path, e);
        }
    }

    public void increment(String path, long count) {
        try {
            ensureCounterExists(path);
            Long value = Longs.fromByteArray(curatorClient.getData().forPath(path));
            Long newValue = value + count;
            curatorClient.setData().forPath(path, Longs.toByteArray(newValue));
        } catch (Exception e) {
            throw new ZookeeperCounterException(path, e);
        }
    }

    public long getValue(String basePath, String childrenPath) {
        Long sum = 0L;
        try {
            for (String child : curatorClient.getChildren().forPath(basePath)) {
                try {
                    sum += Longs.fromByteArray(curatorClient.getData().forPath(basePath + "/" + child + childrenPath));
                } catch (KeeperException.NoNodeException e) {
                    // this is fine, trust me - there is no other way to know if it exists for sure
                    // and i don't care if it doesn't, i want to sum it all
                    logger.trace("Someone removed node " + basePath + "/" + child + childrenPath + " while we were iterating", e);
                }
            }
        } catch (Exception e) {
            logger.error(String.format("Error while reading value for path %s/?/%s", basePath, childrenPath), e);
        } finally {
            return sum;
        }
    }

    private void ensureCounterExists(String path) {
        try {
            if (curatorClient.checkExists().creatingParentContainersIfNeeded().forPath(path) == null) {
                curatorClient.create().withMode(CreateMode.EPHEMERAL).forPath(path, Longs.toByteArray(0));
            }
        } catch (Exception e) {
            throw new ZookeeperCounterException(path, e);
        }
    }

    public int countOccurrences(String basePath, String childrenPath) {
        int count = 0;
        try {
            for (String child : curatorClient.getChildren().forPath(basePath)) {
                if (curatorClient.checkExists().forPath(basePath + "/" + child + childrenPath) != null) {
                    count += 1;
                }
            }
            return count;
        } catch (KeeperException.NoNodeException e) {
            return count;
        } catch (Exception e) {
            throw new ZookeeperCounterException(basePath + "/?/" + childrenPath, e);
        }
    }

}
