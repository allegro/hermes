package pl.allegro.tech.hermes.management.domain.detection;

import org.springframework.stereotype.Service;
import pl.allegro.tech.hermes.management.domain.dc.MultiDatacenterRepositoryCommandExecutor;
import pl.allegro.tech.hermes.management.domain.detection.command.MarkTopicsAsUnusedRepositoryCommand;

import java.util.List;

@Service
public class UnusedTopicsService {
  private final UnusedTopicsRepository unusedTopicsRepository;
  private final MultiDatacenterRepositoryCommandExecutor multiDcExecutor;

  public UnusedTopicsService(
      UnusedTopicsRepository unusedTopicsRepository,
      MultiDatacenterRepositoryCommandExecutor multiDcExecutor) {
    this.unusedTopicsRepository = unusedTopicsRepository;
    this.multiDcExecutor = multiDcExecutor;
  }

  public void markAsUnused(List<UnusedTopic> unusedTopics) {
    multiDcExecutor.execute(new MarkTopicsAsUnusedRepositoryCommand(unusedTopics));
  }

  public List<UnusedTopic> getUnusedTopics() {
    return unusedTopicsRepository.read();
  }
}
