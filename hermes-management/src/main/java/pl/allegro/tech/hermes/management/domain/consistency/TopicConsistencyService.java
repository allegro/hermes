package pl.allegro.tech.hermes.management.domain.consistency;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.management.config.kafka.KafkaClustersProperties;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService;

@Component
public class TopicConsistencyService {

    private final static String AVRO_SUFFIX = "_avro";
    private final TopicService topicService;
    private final MultiDCAwareService multiDCAwareService;
    private final KafkaClustersProperties kafkaClustersProperties;

    public TopicConsistencyService(
        TopicService topicService,
        MultiDCAwareService multiDCAwareService,
        KafkaClustersProperties kafkaClustersProperties) {
        this.topicService = topicService;
        this.kafkaClustersProperties = kafkaClustersProperties;
        this.multiDCAwareService = multiDCAwareService;
    }

    public Set<String> listInconsistentTopics() {
        List<String> topicsFromHermes = topicService.listQualifiedTopicNames();

        return multiDCAwareService.listTopicFromAllDC()
            .stream().map(this::mapTopicName)
            .filter(topic -> !topicsFromHermes.contains(topic))
            .collect(Collectors.toSet());
    }

    private String mapTopicName(String topic) {
        String prefix = kafkaClustersProperties.getDefaultNamespace() + kafkaClustersProperties.getNamespaceSeparator();
        if (topic.endsWith(AVRO_SUFFIX)) {
            topic = topic.substring(0, topic.length() - 5);
        }

        if (topic.startsWith(prefix)) {
            topic = topic.substring(prefix.length());
        }
        return topic;
    }
}
