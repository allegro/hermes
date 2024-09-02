package pl.allegro.tech.hermes.management.domain.retransmit;

import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

class DeleteOfflineRetransmissionTaskCommand
    extends RepositoryCommand<OfflineRetransmissionRepository> {
  private final String taskId;

  DeleteOfflineRetransmissionTaskCommand(String taskId) {
    this.taskId = taskId;
  }

  @Override
  public void backup(DatacenterBoundRepositoryHolder<OfflineRetransmissionRepository> holder) {}

  @Override
  public void execute(DatacenterBoundRepositoryHolder<OfflineRetransmissionRepository> holder) {
    holder.getRepository().deleteTask(taskId);
  }

  @Override
  public void rollback(
      DatacenterBoundRepositoryHolder<OfflineRetransmissionRepository> holder,
      Exception exception) {}

  @Override
  public Class<OfflineRetransmissionRepository> getRepositoryType() {
    return OfflineRetransmissionRepository.class;
  }
}
