package pl.allegro.tech.hermes.management.domain.consistency;

import static java.util.Arrays.asList;

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
    private final static List<String> IGNORED_TOPIC = asList("__consumer_offsets");
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
            .stream()
            .filter(topic -> !IGNORED_TOPIC.contains(topic) && !topicsFromHermes
                .contains(mapTopicName(topic)))
            .collect(Collectors.toSet());
    }

    public void removeTopic(String topicName) {
        multiDCAwareService.removeTopicByName(topicName);
    }

    private String mapTopicName(String topic) {
        String prefix = kafkaClustersProperties.getDefaultNamespace() + kafkaClustersProperties.getNamespaceSeparator();
        String topicInHermes = topic;
        if (topicInHermes.endsWith(AVRO_SUFFIX)) {
            topicInHermes = topic.substring(0, topic.length() - 5);
        }

        if (topicInHermes.startsWith(prefix)) {
            topicInHermes = topic.substring(prefix.length());
        }
        return topicInHermes;
    }
}
