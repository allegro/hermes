package pl.allegro.tech.hermes.management.domain.topic;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.management.domain.ManagementException;

public class TopicRemovalDisabledException extends ManagementException {

  public TopicRemovalDisabledException(Topic topic) {
    super(
        String.format(
            "Could not remove topic %s, this operation is currently disabled.",
            topic.getQualifiedName()));
  }

  @Override
  public ErrorCode getCode() {
    return ErrorCode.OPERATION_DISABLED;
  }
}
