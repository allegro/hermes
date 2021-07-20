package pl.allegro.tech.hermes.management.domain.blacklist.commands;

import pl.allegro.tech.hermes.management.domain.blacklist.TopicBlacklistRepository;
import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class RemoveTopicFromBlacklistRepositoryCommand extends RepositoryCommand<TopicBlacklistRepository> {
    private final String qualifiedTopicName;
    private boolean exists = false;

    public RemoveTopicFromBlacklistRepositoryCommand(String qualifiedTopicName) {
        this.qualifiedTopicName = qualifiedTopicName;
    }

    @Override
    public void backup(DatacenterBoundRepositoryHolder<TopicBlacklistRepository> holder) {
        exists = holder.getRepository().isBlacklisted(qualifiedTopicName);
    }

    @Override
    public void execute(DatacenterBoundRepositoryHolder<TopicBlacklistRepository> holder) {
        holder.getRepository().remove(qualifiedTopicName);
    }

    @Override
    public void rollback(DatacenterBoundRepositoryHolder<TopicBlacklistRepository> holder) {
        if (exists) {
            holder.getRepository().add(qualifiedTopicName);
        }
    }

    @Override
    public Class<TopicBlacklistRepository> getRepositoryType() {
        return TopicBlacklistRepository.class;
    }

    @Override
    public String toString() {
        return "RemoveTopicFromBlacklist(" + qualifiedTopicName + ")";
    }
}
