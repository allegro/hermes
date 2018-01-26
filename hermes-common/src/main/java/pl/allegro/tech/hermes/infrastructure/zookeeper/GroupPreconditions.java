package pl.allegro.tech.hermes.infrastructure.zookeeper;

import pl.allegro.tech.hermes.domain.group.GroupNotEmptyException;
import pl.allegro.tech.hermes.domain.group.GroupNotExistsException;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClient;

public class GroupPreconditions {

    private final ZookeeperPaths paths;

    public GroupPreconditions(ZookeeperPaths paths) {
        this.paths = paths;
    }

    public void ensureGroupExists(ZookeeperClient client, String groupName) {
        String path = paths.groupPath(groupName);
        if (!client.pathExists(path)) {
            throw new GroupNotExistsException(groupName);
        }
    }

    public void ensureGroupIsEmpty(ZookeeperClient client, String groupName) {
        String path = paths.topicsPath(groupName);
        if (!client.childrenOf(path).isEmpty()) {
            throw new GroupNotEmptyException(groupName);
        }
    }

}
