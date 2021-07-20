package pl.allegro.tech.hermes.management.domain.blacklist.commands;

import pl.allegro.tech.hermes.management.domain.blacklist.TopicBlacklistRepository;
import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class AddTopicToBlacklistRepositoryCommand extends RepositoryCommand<TopicBlacklistRepository> {

    private final String qualifiedTopicName;

    public AddTopicToBlacklistRepositoryCommand(String qualifiedTopicName) {
        this.qualifiedTopicName = qualifiedTopicName;
    }

    @Override
    public void backup(DatacenterBoundRepositoryHolder<TopicBlacklistRepository> holder) {}

    @Override
    public void execute(DatacenterBoundRepositoryHolder<TopicBlacklistRepository> holder) {
        holder.getRepository().add(qualifiedTopicName);
    }

    @Override
    public void rollback(DatacenterBoundRepositoryHolder<TopicBlacklistRepository> holder) {
        holder.getRepository().remove(qualifiedTopicName);
    }

    @Override
    public Class<TopicBlacklistRepository> getRepositoryType() {
        return TopicBlacklistRepository.class;
    }

    @Override
    public String toString() {
        return "AddTopicToBlacklist(" + qualifiedTopicName + ")";
    }
}
