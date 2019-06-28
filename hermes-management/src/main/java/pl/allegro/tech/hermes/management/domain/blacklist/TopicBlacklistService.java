package pl.allegro.tech.hermes.management.domain.blacklist;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.management.domain.blacklist.commands.AddTopicToBlacklistRepositoryCommand;
import pl.allegro.tech.hermes.management.domain.blacklist.commands.RemoveTopicFromBlacklistRepositoryCommand;
import pl.allegro.tech.hermes.management.domain.dc.MultiDcRepositoryCommandExecutor;

import java.util.List;

@Component
public class TopicBlacklistService {

    private final TopicBlacklistRepository repository;
    private final MultiDcRepositoryCommandExecutor multiDcExecutor;

    @Autowired
    public TopicBlacklistService(TopicBlacklistRepository repository,
                                 MultiDcRepositoryCommandExecutor multiDcExecutor) {
        this.repository = repository;
        this.multiDcExecutor = multiDcExecutor;
    }

    public void blacklist(String qualifiedTopicName) {
        multiDcExecutor.execute(new AddTopicToBlacklistRepositoryCommand(qualifiedTopicName));
    }

    public void unblacklist(String qualifiedTopicName) {
        multiDcExecutor.execute(new RemoveTopicFromBlacklistRepositoryCommand(qualifiedTopicName));
    }

    public boolean isBlacklisted(String qualifiedTopicName) {
        return repository.isBlacklisted(qualifiedTopicName);
    }

    public List<String> list() {
        return repository.list();
    }

}
