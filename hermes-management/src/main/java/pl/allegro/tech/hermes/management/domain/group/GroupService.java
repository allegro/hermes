package pl.allegro.tech.hermes.management.domain.group;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.helpers.Patch;
import pl.allegro.tech.hermes.api.Query;
import pl.allegro.tech.hermes.domain.group.GroupNotExistsException;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.infrastructure.MalformedDataException;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class GroupService {

    private static final Logger logger = LoggerFactory.getLogger(GroupService.class);

    private final GroupRepository groupRepository;

    @Autowired
    public GroupService(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
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

    public String createGroup(Group group) {
        groupRepository.createGroup(group);
        return "";
    }

    public void removeGroup(String groupName) {
        groupRepository.removeGroup(groupName);
    }

    public void checkGroupExists(String groupName) {
        if (!groupRepository.groupExists(groupName)) {
            throw new GroupNotExistsException(groupName);
        }
    }

    public void updateGroup(String groupName, PatchData patch) {
        try {
            Group retrieved = groupRepository.getGroupDetails(groupName);
            Group modified = Patch.apply(retrieved, patch);
            groupRepository.updateGroup(modified);
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
