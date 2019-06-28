package pl.allegro.tech.hermes.management.infrastructure.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

public class ZookeeperClient {

    private final CuratorFramework curatorFramework;
    private final String dcName;

    public ZookeeperClient(CuratorFramework curatorFramework, String dcName) {
        this.curatorFramework = curatorFramework;
        this.dcName = dcName;
    }

    public CuratorFramework getCuratorFramework() {
        return curatorFramework;
    }

    public String getDcName() {
        return dcName;
    }

    public void ensurePathExists(String path) {
        try {
            if(curatorFramework.checkExists().forPath(path) ==  null) {
                curatorFramework.create().creatingParentsIfNeeded().forPath(path);
            }
        } catch (Exception e) {
            throw new InternalProcessingException("Could not ensure existence of path: " + path);
        }
    }
}
