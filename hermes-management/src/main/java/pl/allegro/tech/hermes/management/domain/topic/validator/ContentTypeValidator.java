package pl.allegro.tech.hermes.management.domain.topic.validator;

import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.management.config.TopicProperties;

@Component
public class ContentTypeValidator {

  private static final String ERROR_MESSAGE =
      "Content type %s is not within allowed content types %s";

  private final Set<ContentType> allowedContentTypes;

  public ContentTypeValidator(TopicProperties topicProperties) {
    this.allowedContentTypes = EnumSet.copyOf(topicProperties.getAllowedContentTypes());
  }

  public void check(ContentType contentType) {
    if (!allowedContentTypes.contains(contentType)) {
      throw new TopicValidationException(
          String.format(ERROR_MESSAGE, contentType, allowedContentTypes));
    }
  }
}
