package pl.allegro.tech.hermes.management.domain.group;

import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.domain.group.GroupAlreadyExistsException;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.management.api.auth.CreatorRights;
import pl.allegro.tech.hermes.management.config.GroupProperties;
import pl.allegro.tech.hermes.management.domain.PermissionDeniedException;

@Component
public class GroupValidator {

  private final GroupRepository repository;

  private final GroupNameValidator groupNameValidator;

  public GroupValidator(GroupRepository repository, GroupProperties groupProperties) {
    this.repository = repository;
    this.groupNameValidator = new GroupNameValidator(groupProperties.getAllowedGroupNameRegex());
  }

  public void checkCreation(Group toCheck, CreatorRights<Group> creatorRights) {
    groupNameValidator.requireValid(toCheck.getGroupName());

    if (!creatorRights.allowedToCreate(toCheck)) {
      throw new PermissionDeniedException("You are not allowed to create groups");
    }

    if (repository.groupExists(toCheck.getGroupName())) {
      throw new GroupAlreadyExistsException(toCheck.getGroupName());
    }
  }
}
