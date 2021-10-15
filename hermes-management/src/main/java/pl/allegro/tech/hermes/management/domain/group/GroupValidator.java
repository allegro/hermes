package pl.allegro.tech.hermes.management.domain.group;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.domain.group.GroupAlreadyExistsException;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.management.api.auth.CreatorRights;
import pl.allegro.tech.hermes.management.config.GroupProperties;
import pl.allegro.tech.hermes.management.domain.GroupNameIsNotAllowedException;
import pl.allegro.tech.hermes.management.domain.PermissionDeniedException;

@Component
public class GroupValidator {

    private final GroupRepository repository;

    private final StringRegexValidator stringRegexValidator;

    public GroupValidator(GroupRepository repository, GroupProperties groupProperties) {
        this.repository = repository;
        this.stringRegexValidator = new StringRegexValidator(groupProperties.getAllowedGroupNameRegex());
    }

    public void checkCreation(Group toCheck, CreatorRights<Group> creatorRights) {
        if (!stringRegexValidator.isValid(toCheck.getGroupName())) {
            throw new GroupNameIsNotAllowedException();
        }

        if (!creatorRights.allowedToCreate(toCheck)) {
            throw new PermissionDeniedException("You are not allowed to create groups");
        }

        if (repository.groupExists(toCheck.getGroupName())) {
            throw new GroupAlreadyExistsException(toCheck.getGroupName());
        }
    }
}
