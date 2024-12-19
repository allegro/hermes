package pl.allegro.tech.hermes.management.domain.detection;

import java.util.List;
import org.springframework.stereotype.Service;
import pl.allegro.tech.hermes.management.domain.dc.MultiDatacenterRepositoryCommandExecutor;
import pl.allegro.tech.hermes.management.domain.detection.command.MarkTopicsAsInactiveRepositoryCommand;

@Service
public class InactiveTopicsStorageService {
  private final InactiveTopicsRepository inactiveTopicsRepository;
  private final MultiDatacenterRepositoryCommandExecutor multiDcExecutor;

  public InactiveTopicsStorageService(
      InactiveTopicsRepository inactiveTopicsRepository,
      MultiDatacenterRepositoryCommandExecutor multiDcExecutor) {
    this.inactiveTopicsRepository = inactiveTopicsRepository;
    this.multiDcExecutor = multiDcExecutor;
  }

  public void markAsInactive(List<InactiveTopic> inactiveTopics) {
    multiDcExecutor.execute(new MarkTopicsAsInactiveRepositoryCommand(inactiveTopics));
  }

  public List<InactiveTopic> getInactiveTopics() {
    return inactiveTopicsRepository.read();
  }
}
