package pl.allegro.tech.hermes.management.domain.readiness;

import java.util.List;
import pl.allegro.tech.hermes.api.DatacenterReadiness;
import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class SetReadinessCommand extends RepositoryCommand<DatacenterReadinessRepository> {
  private final List<DatacenterReadiness> readiness;

  public SetReadinessCommand(List<DatacenterReadiness> readiness) {
    this.readiness = readiness;
  }

  @Override
  public void backup(DatacenterBoundRepositoryHolder<DatacenterReadinessRepository> holder) {}

  @Override
  public void execute(DatacenterBoundRepositoryHolder<DatacenterReadinessRepository> holder) {
    holder.getRepository().setReadiness(readiness);
  }

  @Override
  public void rollback(
      DatacenterBoundRepositoryHolder<DatacenterReadinessRepository> holder, Exception exception) {}

  @Override
  public Class<DatacenterReadinessRepository> getRepositoryType() {
    return DatacenterReadinessRepository.class;
  }

  @Override
  public String toString() {
    return "SetReadinessCommand(" + readiness.toString() + ")";
  }
}
