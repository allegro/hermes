package pl.allegro.tech.hermes.management.domain.group;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.helpers.Patch;
import pl.allegro.tech.hermes.domain.group.GroupNotExistsException;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.infrastructure.MalformedDataException;

import java.util.List;

@Service
public class GroupService {

    private static final Logger logger = LoggerFactory.getLogger(GroupService.class);

    private final GroupRepository groupRepository;

    @Autowired
    public GroupService(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    public List<String> listGroups() {
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

    public void updateGroup(Group group) {
        try {
            Group retrieved = groupRepository.getGroupDetails(group.getGroupName());
            Group modified = Patch.apply(retrieved, group);
            groupRepository.updateGroup(modified);
        } catch (MalformedDataException exception) {
            logger.warn("Problem with reading details of group {}. Overriding them.", group.getGroupName());
            groupRepository.updateGroup(group);
        }

    }
}
