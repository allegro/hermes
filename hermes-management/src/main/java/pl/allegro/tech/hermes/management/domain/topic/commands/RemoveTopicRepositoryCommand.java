package pl.allegro.tech.hermes.management.domain.topic.commands;

import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class RemoveTopicRepositoryCommand extends RepositoryCommand<TopicRepository> {

  private final TopicName topicName;

  public RemoveTopicRepositoryCommand(TopicName topicName) {
    this.topicName = topicName;
  }

  @Override
  public void backup(DatacenterBoundRepositoryHolder<TopicRepository> holder) {}

  @Override
  public void execute(DatacenterBoundRepositoryHolder<TopicRepository> holder) {
    holder.getRepository().removeTopic(topicName);
  }

  @Override
  public void rollback(
      DatacenterBoundRepositoryHolder<TopicRepository> holder, Exception exception) {
    /*
    We don't want to do a rollback due to possible race conditions with creating a topic on Kafka.
    It increases the probability of discrepancies between Kafka and Zookeeper: topic exists in Kafka,
    but not in the Zookeeper and vice versa.
     */
  }

  @Override
  public Class<TopicRepository> getRepositoryType() {
    return TopicRepository.class;
  }

  @Override
  public String toString() {
    return "RemoveTopic(" + topicName + ")";
  }
}
