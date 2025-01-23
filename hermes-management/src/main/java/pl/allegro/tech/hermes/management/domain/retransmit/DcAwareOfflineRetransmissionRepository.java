package pl.allegro.tech.hermes.management.domain.retransmit;

import java.util.List;
import pl.allegro.tech.hermes.api.OfflineRetransmissionTask;
import pl.allegro.tech.hermes.management.domain.dc.MultiDatacenterRepositoryCommandExecutor;

public class DcAwareOfflineRetransmissionRepository implements OfflineRetransmissionRepository {
  private final MultiDatacenterRepositoryCommandExecutor commandExecutor;
  private final OfflineRetransmissionRepository offlineRetransmissionRepository;

  public DcAwareOfflineRetransmissionRepository(
      MultiDatacenterRepositoryCommandExecutor commandExecutor,
      OfflineRetransmissionRepository offlineRetransmissionRepository) {
    this.commandExecutor = commandExecutor;
    this.offlineRetransmissionRepository = offlineRetransmissionRepository;
  }

  @Override
  public void saveTask(OfflineRetransmissionTask task) {
    commandExecutor.execute(new CreateOfflineRetransmissionTaskCommand(task));
  }

  @Override
  public List<OfflineRetransmissionTask> getAllTasks() {
    return offlineRetransmissionRepository.getAllTasks();
  }

  @Override
  public void deleteTask(String taskId) {
    commandExecutor.execute(new DeleteOfflineRetransmissionTaskCommand(taskId));
  }
}
