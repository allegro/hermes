package pl.allegro.tech.hermes.management.domain.topic.commands;

import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class TouchTopicRepositoryCommand extends RepositoryCommand<TopicRepository> {

    private final TopicName topicName;

    public TouchTopicRepositoryCommand(TopicName topicName) {
        this.topicName = topicName;
    }

    @Override
    public void backup(DatacenterBoundRepositoryHolder<TopicRepository> holder) {}

    @Override
    public void execute(DatacenterBoundRepositoryHolder<TopicRepository> holder) {
        holder.getRepository().touchTopic(topicName);
    }

    @Override
    public void rollback(DatacenterBoundRepositoryHolder<TopicRepository> holder) {}

    @Override
    public Class<TopicRepository> getRepositoryType() {
        return TopicRepository.class;
    }

    @Override
    public String toString() {
        return "TouchTopic(" + topicName + ")";
    }
}
