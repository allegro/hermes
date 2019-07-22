package pl.allegro.tech.hermes.management.infrastructure.metrics;

import com.google.common.primitives.Longs;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.apache.commons.lang.exception.ExceptionUtils.getRootCauseMessage;

public class SummedDistributedEphemeralCounter {

    private static final Logger logger = LoggerFactory.getLogger(SummedDistributedEphemeralCounter.class);

    private final List<CuratorFramework> curatorClients;

    public SummedDistributedEphemeralCounter(List<CuratorFramework> curatorClients) {
        this.curatorClients = curatorClients;
    }

    public long getValue(String basePath, String childrenPath) {
        return curatorClients.stream()
                .map(client -> getValue(client, basePath, childrenPath))
                .reduce(0L, (a, b) -> a + b);
    }

    private long getValue(CuratorFramework curatorClient, String basePath, String childrenPath) {
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
            logger.warn("Error while reading value for base path: {} and child path: {}; {}", basePath, childrenPath, getRootCauseMessage(e));
        } finally {
            return sum;
        }
    }
}
