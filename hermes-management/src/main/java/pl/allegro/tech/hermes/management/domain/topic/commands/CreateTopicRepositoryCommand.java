package pl.allegro.tech.hermes.management.domain.topic.commands;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class CreateTopicRepositoryCommand extends RepositoryCommand<TopicRepository> {

    private final Topic topic;

    public CreateTopicRepositoryCommand(Topic topic) {
        this.topic = topic;
    }

    @Override
    public void backup(DatacenterBoundRepositoryHolder<TopicRepository> holder) {}

    @Override
    public void execute(DatacenterBoundRepositoryHolder<TopicRepository> holder) {
        holder.getRepository().createTopic(topic);
    }

    @Override
    public void rollback(DatacenterBoundRepositoryHolder<TopicRepository> holder) {
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
        return "CreateTopic(" + topic.getQualifiedName() + ")";
    }
}
