package pl.allegro.tech.hermes.common.kafka;

import static pl.allegro.tech.hermes.api.helpers.Replacer.replaceInAll;

import com.google.common.base.Joiner;
import java.util.function.Function;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.Topic;

public class NamespaceKafkaNamesMapper implements KafkaNamesMapper {

  private final String namespace;
  private final String namespaceSeparator;

  public NamespaceKafkaNamesMapper(String namespace, String namespaceSeparator) {
    this.namespace = namespace;
    this.namespaceSeparator = namespaceSeparator;
  }

  @Override
  public ConsumerGroupId toConsumerGroupId(SubscriptionName subscriptionName) {
    return ConsumerGroupId.valueOf(appendNamespace(subscriptionNameToConsumerId(subscriptionName)));
  }

  @Override
  public KafkaTopics toKafkaTopics(Topic topic) {
    return mapToKafkaTopic.andThen(appendNamespace).andThen(mapToKafkaTopics).apply(topic);
  }

  protected Function<Topic, KafkaTopic> mapToKafkaTopic =
      it -> new KafkaTopic(KafkaTopicName.valueOf(it.getQualifiedName()), it.getContentType());

  protected Function<KafkaTopic, KafkaTopic> appendNamespace =
      it ->
          new KafkaTopic(
              KafkaTopicName.valueOf(appendNamespace(it.name().asString())), it.contentType());

  protected Function<KafkaTopic, KafkaTopics> mapToKafkaTopics = KafkaTopics::new;

  private String subscriptionNameToConsumerId(SubscriptionName subscriptionName) {
    return Joiner.on("_")
        .join(
            replaceInAll(
                "_",
                "__",
                subscriptionName.getTopicName().getGroupName(),
                subscriptionName.getTopicName().getName(),
                subscriptionName.getName()));
  }

  private String appendNamespace(String name) {
    return namespace.isEmpty() ? name : namespace + namespaceSeparator + name;
  }
}
