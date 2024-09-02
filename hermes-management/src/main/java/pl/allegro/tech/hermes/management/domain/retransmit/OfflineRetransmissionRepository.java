package pl.allegro.tech.hermes.management.domain.retransmit;

import java.util.List;
import pl.allegro.tech.hermes.api.OfflineRetransmissionTask;

public interface OfflineRetransmissionRepository {
  void saveTask(OfflineRetransmissionTask task);

  List<OfflineRetransmissionTask> getAllTasks();

  void deleteTask(String taskId);
}
