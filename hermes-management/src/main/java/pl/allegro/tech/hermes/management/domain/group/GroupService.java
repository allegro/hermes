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

import java.util.List;
import java.util.stream.Collectors;

@Component
public class GroupService {

    private static final Logger logger = LoggerFactory.getLogger(GroupService.class);

    private final GroupRepository groupRepository;
    private final Auditor auditor;

    @Autowired
    public GroupService(GroupRepository groupRepository, Auditor auditor) {
        this.groupRepository = groupRepository;
        this.auditor = auditor;
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
        groupRepository.createGroup(group);
        auditor.objectCreated(createdBy, group);
    }

    public void removeGroup(String groupName, String removedBy) {
        groupRepository.removeGroup(groupName);
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
