package pl.allegro.tech.hermes.test.helper.zookeeper;

import org.apache.curator.framework.CuratorFramework;

public class ZookeeperDataSaver {

    public static void save(CuratorFramework curatorFramework, String path, byte[] data) {
        try {
            if (curatorFramework.checkExists().forPath(path) == null) {
                curatorFramework.create().creatingParentsIfNeeded().forPath(path, data);
            } else {
                curatorFramework.setData().forPath(path, data);
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not store data in zookeeper under path: " + path, e);
        }
    }
}
