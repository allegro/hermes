package pl.allegro.tech.hermes.infrastructure.zookeeper.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.infrastructure.zookeeper.GroupPreconditions;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClient;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommand;

class UpdateGroupZookeeperCommand extends ZookeeperCommand {
    private static final Logger logger = LoggerFactory.getLogger(UpdateGroupZookeeperCommand.class);

    private final Group group;
    private final ObjectMapper mapper;
    private final GroupPreconditions preconditions;

    private byte[] groupDataBackup;

    UpdateGroupZookeeperCommand(Group group, ZookeeperPaths paths, ObjectMapper mapper) {
        super(paths);
        this.group = group;
        this.mapper = mapper;
        this.preconditions = new GroupPreconditions(paths);
    }

    @Override
    public void backup(ZookeeperClient client) {
        String path = paths.groupPath(group.getGroupName());
        groupDataBackup = client.getData(path);
    }

    @Override
    public void execute(ZookeeperClient client) {
        preconditions.ensureGroupExists(client, group.getGroupName());

        logger.info("Updating group '{}' via client '{}'", group.getGroupName(), client.getName());

        String path = paths.groupPath(group.getGroupName());
        client.setData(path, marshall(mapper, group));
    }

    @Override
    public void rollback(ZookeeperClient client) {
        logger.info("Rolling back changes: group '{}' update via client '{}'", group.getGroupName(), client.getName());

        String path = paths.groupPath(group.getGroupName());
        client.setData(path, groupDataBackup);
    }
}
