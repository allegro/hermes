package pl.allegro.tech.hermes.management.domain.topic.commands;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class UpdateTopicRepositoryCommand extends RepositoryCommand<TopicRepository> {

    private final Topic topic;

    private Topic backup;

    public UpdateTopicRepositoryCommand(Topic topic) {
        this.topic = topic;
    }

    @Override
    public void backup(DatacenterBoundRepositoryHolder<TopicRepository> holder) {
        backup = holder.getRepository().getTopicDetails(topic.getName());
    }

    @Override
    public void execute(DatacenterBoundRepositoryHolder<TopicRepository> holder) {
        holder.getRepository().updateTopic(topic);
    }

    @Override
    public void rollback(DatacenterBoundRepositoryHolder<TopicRepository> holder) {
        holder.getRepository().updateTopic(backup);
    }

    @Override
    public Class<TopicRepository> getRepositoryType() {
        return TopicRepository.class;
    }

    @Override
    public String toString() {
        return "UpdateTopic(" + topic.getQualifiedName() + ")";
    }
}
