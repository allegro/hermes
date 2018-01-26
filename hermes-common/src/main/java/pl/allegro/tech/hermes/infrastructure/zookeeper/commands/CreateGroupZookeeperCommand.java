package pl.allegro.tech.hermes.infrastructure.zookeeper.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.domain.group.GroupAlreadyExistsException;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClient;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommand;

class CreateGroupZookeeperCommand extends ZookeeperCommand {
    private static final Logger logger = LoggerFactory.getLogger(CreateGroupZookeeperCommand.class);

    private final Group group;
    private final ObjectMapper mapper;

    CreateGroupZookeeperCommand(Group group, ZookeeperPaths paths, ObjectMapper mapper) {
        super(paths);
        this.group = group;
        this.mapper = mapper;
    }

    @Override
    public void backup(ZookeeperClient client) {}

    @Override
    public void execute(ZookeeperClient client) {
        String groupPath = paths.groupPath(group.getGroupName());
        String topicsPath = paths.topicsPath(group.getGroupName());

        logger.info("Creating group {} for path {} via client {}", group.getGroupName(), groupPath, client.getName());

        CuratorFramework curator = client.getCuratorFramework();
        try {
            curator.transaction().forOperations(
                    curator.transactionOp().create().forPath(groupPath, mapper.writeValueAsBytes(group)),
                    curator.transactionOp().create().forPath(topicsPath)
            );
        } catch (KeeperException.NodeExistsException ex) {
            throw new GroupAlreadyExistsException(group.getGroupName(), ex);
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    @Override
    public void rollback(ZookeeperClient client) {
        String groupPath = paths.groupPath(group.getGroupName());
        client.deleteWithChildren(groupPath);
    }
}
