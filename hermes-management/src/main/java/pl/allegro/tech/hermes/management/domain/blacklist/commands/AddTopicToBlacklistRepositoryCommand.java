package pl.allegro.tech.hermes.management.domain.blacklist.commands;

import pl.allegro.tech.hermes.management.domain.blacklist.TopicBlacklistRepository;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class AddTopicToBlacklistRepositoryCommand extends RepositoryCommand<TopicBlacklistRepository> {

    private final String qualifiedTopicName;

    public AddTopicToBlacklistRepositoryCommand(String qualifiedTopicName) {
        this.qualifiedTopicName = qualifiedTopicName;
    }

    @Override
    public void backup(TopicBlacklistRepository repository) {}

    @Override
    public void execute(TopicBlacklistRepository repository) {
        repository.add(qualifiedTopicName);
    }

    @Override
    public void rollback(TopicBlacklistRepository repository) {
        repository.remove(qualifiedTopicName);
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
