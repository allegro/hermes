package pl.allegro.tech.hermes.management.domain.group.commands;

import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class UpdateGroupRepositoryCommand extends RepositoryCommand<GroupRepository> {

    private final Group group;

    private Group backup;

    public UpdateGroupRepositoryCommand(Group group) {
        this.group = group;
    }

    @Override
    public void backup(GroupRepository repository) {
        backup = repository.getGroupDetails(group.getGroupName());
    }

    @Override
    public void execute(GroupRepository repository) {
        repository.updateGroup(group);
    }

    @Override
    public void rollback(GroupRepository repository) {
        repository.updateGroup(backup);
    }

    @Override
    public Class<GroupRepository> getRepositoryType() {
        return GroupRepository.class;
    }

    @Override
    public String toString() {
        return "UpdateGroup(" + group.getGroupName() + ")";
    }
}
