package pl.allegro.tech.hermes.management.domain.detection.command;

import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;
import pl.allegro.tech.hermes.management.domain.detection.UnusedTopic;
import pl.allegro.tech.hermes.management.domain.detection.UnusedTopicsRepository;

import java.util.List;

public class MarkTopicsAsUnusedRepositoryCommand extends RepositoryCommand<UnusedTopicsRepository> {

  private final List<UnusedTopic> unusedTopics;

  public MarkTopicsAsUnusedRepositoryCommand(List<UnusedTopic> unusedTopics) {
    this.unusedTopics = unusedTopics;
  }

  @Override
  public void backup(DatacenterBoundRepositoryHolder<UnusedTopicsRepository> holder) {
    // TODO
  }

  @Override
  public void execute(DatacenterBoundRepositoryHolder<UnusedTopicsRepository> holder) {
    holder.getRepository().upsert(unusedTopics);
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
