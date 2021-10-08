package pl.allegro.tech.hermes.management.domain.topic.commands;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class RemoveTopicRepositoryCommand extends RepositoryCommand<TopicRepository> {

    private final TopicName topicName;

    private Topic backup;

    public RemoveTopicRepositoryCommand(TopicName topicName) {
        this.topicName = topicName;
    }

    @Override
    public void backup(DatacenterBoundRepositoryHolder<TopicRepository> holder) {
        backup = holder.getRepository().getTopicDetails(topicName);
    }

    @Override
    public void execute(DatacenterBoundRepositoryHolder<TopicRepository> holder) {
        holder.getRepository().removeTopic(topicName);
    }

    @Override
    public void rollback(DatacenterBoundRepositoryHolder<TopicRepository> holder) {
        holder.getRepository().createTopic(backup);
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
