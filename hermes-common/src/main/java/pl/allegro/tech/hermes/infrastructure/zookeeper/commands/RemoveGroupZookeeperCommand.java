package pl.allegro.tech.hermes.infrastructure.zookeeper.commands;

import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.infrastructure.zookeeper.GroupPreconditions;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClient;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommand;

class RemoveGroupZookeeperCommand extends ZookeeperCommand {

    private static final Logger logger = LoggerFactory.getLogger(RemoveGroupZookeeperCommand.class);

    private final String groupName;
    private final GroupPreconditions preconditions;

    private byte[] groupDataBackup;

    RemoveGroupZookeeperCommand(String groupName, ZookeeperPaths paths) {
        super(paths);
        this.groupName = groupName;
        this.preconditions = new GroupPreconditions(paths);
    }

    @Override
    public void backup(ZookeeperClient client) {
        groupDataBackup = client.getData(paths.groupPath(groupName));
    }

    @Override
    public void execute(ZookeeperClient client) {
        preconditions.ensureGroupExists(client, groupName);
        preconditions.ensureGroupIsEmpty(client, groupName);

        logger.info("Removing group {} via client {}", groupName, client.getName());
        client.deleteWithChildrenWithGuarantee(paths.groupPath(groupName));
    }

    @Override
    public void rollback(ZookeeperClient client) {
        CuratorFramework curator = client.getCuratorFramework();
        try {
            curator.transaction().forOperations(
                    curator.transactionOp().create().forPath(paths.groupPath(groupName), groupDataBackup),
                    curator.transactionOp().create().forPath(paths.topicsPath(groupName))
            );
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }
}
