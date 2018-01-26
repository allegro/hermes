package pl.allegro.tech.hermes.infrastructure.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.domain.group.GroupNotExistsException;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClient;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClientManager;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommand;
import pl.allegro.tech.hermes.infrastructure.zookeeper.commands.ZookeeperCommandFactory;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommandExecutor;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommandFailedException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DistributedZookeeperGroupRepository extends DistributedZookeeperRepository implements GroupRepository {

    private final ZookeeperPaths paths;
    private final ZookeeperCommandExecutor commandExecutor;
    private final ZookeeperCommandFactory commandFactory;
    private final GroupPreconditions preconditions;

    public DistributedZookeeperGroupRepository(ZookeeperClientManager clientManager,
                                               ZookeeperCommandExecutor commandExecutor,
                                               ZookeeperCommandFactory commandFactory,
                                               ZookeeperPaths paths,
                                               ObjectMapper mapper) {
        super(clientManager, mapper);
        this.commandExecutor = commandExecutor;
        this.commandFactory = commandFactory;
        this.paths = paths;
        this.preconditions = new GroupPreconditions(paths);
    }

    @Override
    public boolean groupExists(String groupName) {
        ZookeeperClient client = clientManager.getLocalClient();
        client.ensureConnected();

        String path = paths.groupPath(groupName);
        return client.pathExists(path);
    }

    @Override
    public void ensureGroupExists(String groupName) {
        ZookeeperClient client = clientManager.getLocalClient();
        client.ensureConnected();

        preconditions.ensureGroupExists(client, groupName);
    }

    @Override
    public void createGroup(Group group) {
        ZookeeperCommand command = commandFactory.createGroup(group);
        try {
            commandExecutor.execute(command);
        } catch (ZookeeperCommandFailedException e) {
            throw new InternalProcessingException(e);
        }
    }

    @Override
    public void updateGroup(Group group) {
        ZookeeperCommand command = commandFactory.updateGroup(group);
        try {
            commandExecutor.execute(command);
        } catch (ZookeeperCommandFailedException e) {
            throw new InternalProcessingException(e);
        }
    }

    @Override
    public void removeGroup(String groupName) {
        ZookeeperCommand command = commandFactory.removeGroup(groupName);
        try {
            commandExecutor.execute(command);
        } catch (ZookeeperCommandFailedException e) {
            throw new InternalProcessingException(e);
        }
    }

    @Override
    public List<String> listGroupNames() {
        ZookeeperClient client = clientManager.getLocalClient();
        client.ensureConnected();

        return client.childrenOf(paths.groupsPath());
    }

    @Override
    public List<Group> listGroups() {
        return listGroupNames().stream()
                .map(name -> getGroupDetails(name, true))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public Group getGroupDetails(String groupName) {
        return getGroupDetails(groupName, false).get();
    }

    private Optional<Group> getGroupDetails(String groupName, boolean quiet) {
        ZookeeperClient client = clientManager.getLocalClient();

        preconditions.ensureGroupExists(client, groupName);

        String path = paths.groupPath(groupName);
        return client.readFrom(path, data -> unmarshall(data, Group.class), quiet);
    }
}
