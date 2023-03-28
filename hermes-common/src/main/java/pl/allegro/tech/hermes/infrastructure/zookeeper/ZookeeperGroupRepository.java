package pl.allegro.tech.hermes.infrastructure.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.domain.group.GroupAlreadyExistsException;
import pl.allegro.tech.hermes.domain.group.GroupNotEmptyException;
import pl.allegro.tech.hermes.domain.group.GroupNotExistsException;
import pl.allegro.tech.hermes.domain.group.GroupRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ZookeeperGroupRepository extends ZookeeperBasedRepository implements GroupRepository {

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperGroupRepository.class);

    public ZookeeperGroupRepository(CuratorFramework zookeeper,
                                    ObjectMapper mapper,
                                    ZookeeperPaths paths) {
        super(zookeeper, mapper, paths);
    }

    @Override
    public boolean groupExists(String groupName) {
        return pathExists(paths.groupPath(groupName));
    }

    @Override
    public void ensureGroupExists(String groupName) {
        if (!groupExists(groupName)) {
            throw new GroupNotExistsException(groupName);
        }
    }

    @Override
    public void createGroup(Group group) {
        String groupPath = paths.groupPath(group.getGroupName());
        logger.info("Creating group {} for path {}", group.getGroupName(), groupPath);

        try {
            createInTransaction(groupPath, group, paths.topicsPath(group.getGroupName()));
        } catch (KeeperException.NodeExistsException ex) {
            throw new GroupAlreadyExistsException(group.getGroupName(), ex);
        } catch (Exception ex) {
            throw new InternalProcessingException(ex);
        }
    }

    @Override
    public void updateGroup(Group group) {
        ensureGroupExists(group.getGroupName());

        logger.info("Updating group {}", group.getGroupName());
        try {
            overwrite(paths.groupPath(group.getGroupName()), group);
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    @Override
    public void removeGroup(String groupName) {
        ensureGroupExists(groupName);
        ensureGroupIsEmpty(groupName);

        logger.info("Removing group: {}", groupName);
        try {
            remove(paths.groupPath(groupName));
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    private void ensureGroupIsEmpty(String groupName) {
        if (!childrenOf(paths.topicsPath(groupName)).isEmpty()) {
            throw new GroupNotEmptyException(groupName);
        }
    }

    @Override
    public List<String> listGroupNames() {
        return childrenOf(paths.groupsPath());
    }

    @Override
    public List<Group> listGroups() {
        return listGroupNames().stream()
                .map(n -> getGroupDetails(n, true))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public Group getGroupDetails(String groupName) {
        return getGroupDetails(groupName, false).get();
    }

    private Optional<Group> getGroupDetails(String groupName, boolean quiet) {
        ensureGroupExists(groupName);

        String path = paths.groupPath(groupName);
        return readFrom(path, Group.class, quiet);
    }
}
