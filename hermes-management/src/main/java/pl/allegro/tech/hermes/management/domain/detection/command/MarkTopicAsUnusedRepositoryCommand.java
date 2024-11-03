package pl.allegro.tech.hermes.management.domain.detection.command;

import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;
import pl.allegro.tech.hermes.management.domain.detection.UnusedTopic;
import pl.allegro.tech.hermes.management.domain.detection.UnusedTopicsRepository;

public class MarkTopicAsUnusedRepositoryCommand extends RepositoryCommand<UnusedTopicsRepository> {

  private final UnusedTopic unusedTopic;

  public MarkTopicAsUnusedRepositoryCommand(UnusedTopic unusedTopic) {
    this.unusedTopic = unusedTopic;
  }

  @Override
  public void backup(DatacenterBoundRepositoryHolder<UnusedTopicsRepository> holder) {
    // TODO
  }

  @Override
  public void execute(DatacenterBoundRepositoryHolder<UnusedTopicsRepository> holder) {
    holder.getRepository().markAsUnused(unusedTopic);
  }

  @Override
  public void rollback(
      DatacenterBoundRepositoryHolder<UnusedTopicsRepository> holder, Exception exception) {
    // TODO
  }

  @Override
  public Class<UnusedTopicsRepository> getRepositoryType() {
    return UnusedTopicsRepository.class;
  }

  @Override
  public String toString() {
    return String.format("MarkTopicAsUnused(%s)", unusedTopic.topicName().qualifiedName());
  }
}
