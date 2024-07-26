package pl.allegro.tech.hermes.management.domain.topic.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class RemoveTopicRepositoryCommand extends RepositoryCommand<TopicRepository> {

    private final TopicName topicName;
    private static final Logger logger = LoggerFactory.getLogger(RemoveTopicRepositoryCommand.class);

    public RemoveTopicRepositoryCommand(TopicName topicName) {
        this.topicName = topicName;
    }

    @Override
    public void backup(DatacenterBoundRepositoryHolder<TopicRepository> holder) {}

    @Override
    public void execute(DatacenterBoundRepositoryHolder<TopicRepository> holder) {
        logger.info("Removing topic: {} in ZK dc: {}", topicName, holder.getDatacenterName());
        long start = System.currentTimeMillis();
        holder.getRepository().removeTopic(topicName);
        logger.info("Removed topic: {} in ZK dc: {}, in {} ms", topicName, holder.getDatacenterName(), System.currentTimeMillis() - start);

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
        return "RemoveTopic(" + topicName + ")";
    }
}
