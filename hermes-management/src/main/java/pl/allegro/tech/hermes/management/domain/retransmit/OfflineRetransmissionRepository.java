package pl.allegro.tech.hermes.management.domain.retransmit;

import pl.allegro.tech.hermes.api.OfflineRetransmissionTask;

import java.util.List;

public interface OfflineRetransmissionRepository {
    void saveTask(OfflineRetransmissionTask task);

    List<OfflineRetransmissionTask> getAllTasks();

    void deleteTask(String taskId);
}
