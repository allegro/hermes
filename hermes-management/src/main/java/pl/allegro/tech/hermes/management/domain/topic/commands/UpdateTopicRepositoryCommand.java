package pl.allegro.tech.hermes.management.domain.topic.commands;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class UpdateTopicRepositoryCommand extends RepositoryCommand<TopicRepository> {

    private final Topic topic;

    private Topic backup;

    public UpdateTopicRepositoryCommand(Topic topic) {
        this.topic = topic;
    }

    @Override
    public void backup(TopicRepository repository) {
        backup = repository.getTopicDetails(topic.getName());
    }

    @Override
    public void execute(TopicRepository repository) {
        repository.updateTopic(topic);
    }

    @Override
    public void rollback(TopicRepository repository) {
        repository.updateTopic(backup);
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
