package pl.allegro.tech.hermes.management.domain.topic.validator;

import java.util.Set;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.TopicLabel;
import pl.allegro.tech.hermes.management.config.TopicProperties;

@Component
public class TopicLabelsValidator {

  private static final String ERROR_MESSAGE =
      "One of topic labels %s is not within allowed topic labels %s";

  private final Set<TopicLabel> allowedTopicLabels;

  public TopicLabelsValidator(TopicProperties topicProperties) {
    this.allowedTopicLabels = topicProperties.getAllowedTopicLabels();
  }

  public void check(Set<TopicLabel> labels) {
    if (!allowedTopicLabels.containsAll(labels)) {
      throw new TopicValidationException(String.format(ERROR_MESSAGE, labels, allowedTopicLabels));
    }
  }
}
