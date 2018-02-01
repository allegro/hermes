package pl.allegro.tech.hermes.management.domain.topic.commands;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryCommand;

public class RemoveTopicRepositoryCommand extends RepositoryCommand<TopicRepository> {

    private final TopicName topicName;

    private Topic backup;

    public RemoveTopicRepositoryCommand(TopicName topicName) {
        this.topicName = topicName;
    }

    @Override
    public void backup(TopicRepository repository) {
        backup = repository.getTopicDetails(topicName);
    }

    @Override
    public void execute(TopicRepository repository) {
        repository.removeTopic(topicName);
    }

    @Override
    public void rollback(TopicRepository repository) {
        repository.createTopic(backup);
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
