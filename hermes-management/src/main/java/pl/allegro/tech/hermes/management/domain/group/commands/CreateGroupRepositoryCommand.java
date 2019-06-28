package pl.allegro.tech.hermes.management.domain.group.commands;

import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class CreateGroupRepositoryCommand extends RepositoryCommand<GroupRepository> {

    private final Group group;

    public CreateGroupRepositoryCommand(Group group) {
        this.group = group;
    }

    @Override
    public void backup(GroupRepository repository) {}

    @Override
    public void execute(GroupRepository repository) {
        repository.createGroup(group);
    }

    @Override
    public void rollback(GroupRepository repository) {
        repository.removeGroup(group.getGroupName());
    }

    @Override
    public Class<GroupRepository> getRepositoryType() {
        return GroupRepository.class;
    }

    @Override
    public String toString() {
        return "CreateGroup(" + group.getGroupName() + ")";
    }
}
