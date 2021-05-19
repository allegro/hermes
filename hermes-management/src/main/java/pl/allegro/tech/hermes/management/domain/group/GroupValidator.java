package pl.allegro.tech.hermes.management.domain.group;

import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.domain.group.GroupAlreadyExistsException;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.management.api.auth.CreatorRights;
import pl.allegro.tech.hermes.management.domain.PermissionDeniedException;

@Component
public class GroupValidator {

    private final GroupRepository repository;

    public GroupValidator(GroupRepository repository) {
        this.repository = repository;
    }

    public void checkCreation(Group toCheck, CreatorRights<Group> creatorRights) {
        if (!creatorRights.allowedToCreate(toCheck)) {
            throw new PermissionDeniedException("You are not allowed to create groups");
        }
        if (repository.groupExists(toCheck.getGroupName())) {
            throw new GroupAlreadyExistsException(toCheck.getGroupName());
        }
    }

}
