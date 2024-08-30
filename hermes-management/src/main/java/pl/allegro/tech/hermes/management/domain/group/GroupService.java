package pl.allegro.tech.hermes.management.domain.group;

import java.util.List;
import java.util.stream.Collectors;
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
import pl.allegro.tech.hermes.management.api.auth.CreatorRights;
import pl.allegro.tech.hermes.management.domain.Auditor;
import pl.allegro.tech.hermes.management.domain.auth.RequestUser;
import pl.allegro.tech.hermes.management.domain.dc.MultiDatacenterRepositoryCommandExecutor;
import pl.allegro.tech.hermes.management.domain.group.commands.CreateGroupRepositoryCommand;
import pl.allegro.tech.hermes.management.domain.group.commands.RemoveGroupRepositoryCommand;
import pl.allegro.tech.hermes.management.domain.group.commands.UpdateGroupRepositoryCommand;

@Component
public class GroupService {

  private static final Logger logger = LoggerFactory.getLogger(GroupService.class);

  private final GroupRepository groupRepository;
  private final Auditor auditor;
  private final MultiDatacenterRepositoryCommandExecutor multiDcExecutor;
  private final GroupValidator validator;

  @Autowired
  public GroupService(
      GroupRepository groupRepository,
      Auditor auditor,
      MultiDatacenterRepositoryCommandExecutor multiDcExecutor,
      GroupValidator validator) {
    this.groupRepository = groupRepository;
    this.auditor = auditor;
    this.multiDcExecutor = multiDcExecutor;
    this.validator = validator;
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

  public void createGroup(Group group, RequestUser createdBy, CreatorRights<Group> creatorRights) {
    validator.checkCreation(group, creatorRights);
    multiDcExecutor.executeByUser(new CreateGroupRepositoryCommand(group), createdBy);
    auditor.objectCreated(createdBy.getUsername(), group);
  }

  public void removeGroup(String groupName, RequestUser removedBy) {
    multiDcExecutor.executeByUser(new RemoveGroupRepositoryCommand(groupName), removedBy);
    auditor.objectRemoved(removedBy.getUsername(), Group.from(groupName));
  }

  public void checkGroupExists(String groupName) {
    if (!groupRepository.groupExists(groupName)) {
      throw new GroupNotExistsException(groupName);
    }
  }

  public void updateGroup(String groupName, PatchData patch, RequestUser modifiedBy) {
    try {
      Group retrieved = groupRepository.getGroupDetails(groupName);
      Group modified = Patch.apply(retrieved, patch);
      multiDcExecutor.executeByUser(new UpdateGroupRepositoryCommand(modified), modifiedBy);
      groupRepository.updateGroup(modified);
      auditor.objectUpdated(modifiedBy.getUsername(), retrieved, modified);
    } catch (MalformedDataException exception) {
      logger.warn("Problem with reading details of group {}.", groupName);
      throw exception;
    }
  }

  public List<Group> queryGroup(Query<Group> query) {
    return query.filter(listGroups()).collect(Collectors.toList());
  }
}
