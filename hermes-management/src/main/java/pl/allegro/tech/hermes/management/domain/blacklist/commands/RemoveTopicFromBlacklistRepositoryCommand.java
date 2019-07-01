package pl.allegro.tech.hermes.management.domain.blacklist.commands;

import pl.allegro.tech.hermes.management.domain.blacklist.TopicBlacklistRepository;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class RemoveTopicFromBlacklistRepositoryCommand extends RepositoryCommand<TopicBlacklistRepository> {
    private final String qualifiedTopicName;
    private boolean exists = false;

    public RemoveTopicFromBlacklistRepositoryCommand(String qualifiedTopicName) {
        this.qualifiedTopicName = qualifiedTopicName;
    }

    @Override
    public void backup(TopicBlacklistRepository repository) {
        exists = repository.isBlacklisted(qualifiedTopicName);
    }

    @Override
    public void execute(TopicBlacklistRepository repository) {
        repository.remove(qualifiedTopicName);
    }

    @Override
    public void rollback(TopicBlacklistRepository repository) {
        if (exists) {
            repository.add(qualifiedTopicName);
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
