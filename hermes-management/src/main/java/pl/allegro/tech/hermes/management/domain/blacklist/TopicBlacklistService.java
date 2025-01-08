package pl.allegro.tech.hermes.management.domain.blacklist;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.management.domain.auth.RequestUser;
import pl.allegro.tech.hermes.management.domain.blacklist.commands.AddTopicToBlacklistRepositoryCommand;
import pl.allegro.tech.hermes.management.domain.blacklist.commands.RemoveTopicFromBlacklistRepositoryCommand;
import pl.allegro.tech.hermes.management.domain.dc.MultiDatacenterRepositoryCommandExecutor;

@Component
public class TopicBlacklistService {

  private final TopicBlacklistRepository repository;
  private final MultiDatacenterRepositoryCommandExecutor multiDcExecutor;

  @Autowired
  public TopicBlacklistService(
      TopicBlacklistRepository repository,
      MultiDatacenterRepositoryCommandExecutor multiDcExecutor) {
    this.repository = repository;
    this.multiDcExecutor = multiDcExecutor;
  }

  public void blacklist(String qualifiedTopicName, RequestUser blacklistRequester) {
    multiDcExecutor.executeByUser(
        new AddTopicToBlacklistRepositoryCommand(qualifiedTopicName), blacklistRequester);
  }

  public void unblacklist(String qualifiedTopicName, RequestUser unblacklistRequester) {
    multiDcExecutor.executeByUser(
        new RemoveTopicFromBlacklistRepositoryCommand(qualifiedTopicName), unblacklistRequester);
  }

  public boolean isBlacklisted(String qualifiedTopicName) {
    return repository.isBlacklisted(qualifiedTopicName);
  }

  public List<String> list() {
    return repository.list();
  }
}
