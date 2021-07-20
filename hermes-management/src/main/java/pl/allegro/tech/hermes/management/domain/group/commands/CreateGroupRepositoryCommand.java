package pl.allegro.tech.hermes.management.domain.group.commands;

import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class CreateGroupRepositoryCommand extends RepositoryCommand<GroupRepository> {

    private final Group group;
    private boolean exists;

    public CreateGroupRepositoryCommand(Group group) {
        this.group = group;
    }

    @Override
    public void backup(DatacenterBoundRepositoryHolder<GroupRepository> holder) {
        exists = holder.getRepository().groupExists(group.getGroupName());
    }

    @Override
    public void execute(DatacenterBoundRepositoryHolder<GroupRepository> holder) {
        holder.getRepository().createGroup(group);
    }

    @Override
    public void rollback(DatacenterBoundRepositoryHolder<GroupRepository> holder) {
        if (!exists) {
            holder.getRepository().removeGroup(group.getGroupName());
        }
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
