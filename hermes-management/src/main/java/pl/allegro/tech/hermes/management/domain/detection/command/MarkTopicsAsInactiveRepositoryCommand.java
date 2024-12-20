package pl.allegro.tech.hermes.management.domain.detection.command;

import java.util.List;
import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;
import pl.allegro.tech.hermes.management.domain.detection.InactiveTopic;
import pl.allegro.tech.hermes.management.domain.detection.InactiveTopicsRepository;

public class MarkTopicsAsInactiveRepositoryCommand
    extends RepositoryCommand<InactiveTopicsRepository> {

  private final List<InactiveTopic> inactiveTopics;
  private List<InactiveTopic> backup;

  public MarkTopicsAsInactiveRepositoryCommand(List<InactiveTopic> inactiveTopics) {
    this.inactiveTopics = inactiveTopics;
  }

  @Override
  public void backup(DatacenterBoundRepositoryHolder<InactiveTopicsRepository> holder) {
    backup = holder.getRepository().read();
  }

  @Override
  public void execute(DatacenterBoundRepositoryHolder<InactiveTopicsRepository> holder) {
    holder.getRepository().upsert(inactiveTopics);
  }

  @Override
  public void rollback(
      DatacenterBoundRepositoryHolder<InactiveTopicsRepository> holder, Exception exception) {
    holder.getRepository().upsert(backup);
  }

  @Override
  public Class<InactiveTopicsRepository> getRepositoryType() {
    return InactiveTopicsRepository.class;
  }

  @Override
  public String toString() {
    return String.format("MarkTopicsAsInactive(number of topics=%d)", inactiveTopics.size());
  }
}
