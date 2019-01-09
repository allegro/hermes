package pl.allegro.tech.hermes.tracker.elasticsearch.management;

import org.elasticsearch.client.Client;
import pl.allegro.tech.hermes.api.MessageTrace;
import pl.allegro.tech.hermes.api.SentMessageTrace;
import pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware;
import pl.allegro.tech.hermes.tracker.management.LogRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MultiElasticsearchLogRepository implements LogRepository, LogSchemaAware {

    private List<ElasticsearchLogRepository> elasticsearchLogRepositories;

    public MultiElasticsearchLogRepository(List<Client> elasticClients) {
        elasticsearchLogRepositories = elasticClients.stream()
                .map(ElasticsearchLogRepository::new)
                .collect(Collectors.toList());
    }

    @Override
    public List<SentMessageTrace> getLastUndeliveredMessages(String topicName, String subscriptionName, int limit) {
        return elasticsearchLogRepositories.stream()
                .map(repo -> repo.getLastUndeliveredMessages(topicName, subscriptionName, limit))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    @Override
    public List<MessageTrace> getMessageStatus(String qualifiedTopicName, String subscriptionName, String messageId) {
        return elasticsearchLogRepositories.stream()
                .map(repo -> repo.getMessageStatus(qualifiedTopicName, subscriptionName, messageId))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }
}
