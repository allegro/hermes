package pl.allegro.tech.hermes.management.domain.topic.commands;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class CreateTopicRepositoryCommand extends RepositoryCommand<TopicRepository> {

    private final Topic topic;

    public CreateTopicRepositoryCommand(Topic topic) {
        this.topic = topic;
    }

    @Override
    public void backup(TopicRepository repository) {}

    @Override
    public void execute(TopicRepository repository) {
        repository.createTopic(topic);
    }

    @Override
    public void rollback(TopicRepository repository) {
        repository.removeTopic(topic.getName());
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
