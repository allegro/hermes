package pl.allegro.tech.hermes.management.domain.group.commands;

import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class RemoveGroupRepositoryCommand extends RepositoryCommand<GroupRepository> {

    private final String groupName;

    private Group backup;

    public RemoveGroupRepositoryCommand(String groupName) {
        this.groupName = groupName;
    }

    @Override
    public void backup(GroupRepository repository) {
        backup = repository.getGroupDetails(groupName);
    }

    @Override
    public void execute(GroupRepository repository) {
        repository.removeGroup(groupName);
    }

    @Override
    public void rollback(GroupRepository repository) {
        repository.createGroup(backup);
    }

    @Override
    public Class<GroupRepository> getRepositoryType() {
        return GroupRepository.class;
    }

    @Override
    public String toString() {
        return "RemoveGroup(" + groupName + ")";
    }
}
