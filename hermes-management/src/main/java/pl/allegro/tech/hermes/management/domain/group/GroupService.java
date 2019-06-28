package pl.allegro.tech.hermes.management.domain.group;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.Query;
import pl.allegro.tech.hermes.api.helpers.Patch;
import pl.allegro.tech.hermes.domain.group.GroupNotExistsException;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.infrastructure.MalformedDataException;
import pl.allegro.tech.hermes.management.domain.Auditor;
import pl.allegro.tech.hermes.management.domain.group.commands.CreateGroupRepositoryCommand;
import pl.allegro.tech.hermes.management.domain.group.commands.RemoveGroupRepositoryCommand;
import pl.allegro.tech.hermes.management.domain.group.commands.UpdateGroupRepositoryCommand;
import pl.allegro.tech.hermes.management.domain.dc.MultiDcRepositoryCommandExecutor;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class GroupService {

    private static final Logger logger = LoggerFactory.getLogger(GroupService.class);

    private final GroupRepository groupRepository;
    private final Auditor auditor;
    private final MultiDcRepositoryCommandExecutor multiDcExecutor;

    @Autowired
    public GroupService(GroupRepository groupRepository, Auditor auditor,
                        MultiDcRepositoryCommandExecutor multiDcExecutor) {
        this.groupRepository = groupRepository;
        this.auditor = auditor;
        this.multiDcExecutor = multiDcExecutor;
    }

    public List<Group> listGroups() {
        return groupRepository.listGroups();
    }

    public List<String> listGroupNames() {
        return groupRepository.listGroupNames();
    }

    public Group getGroupDetails(String groupName) {
        return groupRepository.getGroupDetails(groupName);
    }

    public void createGroup(Group group, String createdBy) {
        multiDcExecutor.execute(new CreateGroupRepositoryCommand(group));
        auditor.objectCreated(createdBy, group);
    }

    public void removeGroup(String groupName, String removedBy) {
        multiDcExecutor.execute(new RemoveGroupRepositoryCommand(groupName));
        auditor.objectRemoved(removedBy, Group.class.getSimpleName(), groupName);
    }

    public void checkGroupExists(String groupName) {
        if (!groupRepository.groupExists(groupName)) {
            throw new GroupNotExistsException(groupName);
        }
    }

    public void updateGroup(String groupName, PatchData patch, String modifiedBy) {
        try {
            Group retrieved = groupRepository.getGroupDetails(groupName);
            Group modified = Patch.apply(retrieved, patch);
            multiDcExecutor.execute(new UpdateGroupRepositoryCommand(modified));
            groupRepository.updateGroup(modified);
            auditor.objectUpdated(modifiedBy, retrieved, modified);
        } catch (MalformedDataException exception) {
            logger.warn("Problem with reading details of group {}.", groupName);
            throw exception;
        }
    }

    public List<Group> queryGroup(Query<Group> query) {
        return query
                .filter(listGroups())
                .collect(Collectors.toList());
    }
}
