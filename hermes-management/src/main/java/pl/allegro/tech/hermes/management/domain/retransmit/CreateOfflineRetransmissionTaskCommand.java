package pl.allegro.tech.hermes.management.domain.retransmit;

import pl.allegro.tech.hermes.api.OfflineRetransmissionTask;
import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

class CreateOfflineRetransmissionTaskCommand
    extends RepositoryCommand<OfflineRetransmissionRepository> {
  private final OfflineRetransmissionTask task;

  CreateOfflineRetransmissionTaskCommand(OfflineRetransmissionTask task) {
    this.task = task;
  }

  @Override
  public void backup(DatacenterBoundRepositoryHolder<OfflineRetransmissionRepository> holder) {}

  @Override
  public void execute(DatacenterBoundRepositoryHolder<OfflineRetransmissionRepository> holder) {
    holder.getRepository().saveTask(task);
  }

  @Override
  public void rollback(
      DatacenterBoundRepositoryHolder<OfflineRetransmissionRepository> holder,
      Exception exception) {
    holder.getRepository().deleteTask(task.getTaskId());
  }

  @Override
  public Class<OfflineRetransmissionRepository> getRepositoryType() {
    return OfflineRetransmissionRepository.class;
  }

  @Override
  public String toString() {
    return "CreateOfflineRetransmissionTaskCommand(" + task.getTaskId() + ")";
  }
}
