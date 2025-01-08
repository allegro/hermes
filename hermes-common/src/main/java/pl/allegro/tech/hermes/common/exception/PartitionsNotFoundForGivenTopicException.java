package pl.allegro.tech.hermes.common.exception;

import pl.allegro.tech.hermes.api.ErrorCode;

@SuppressWarnings("serial")
public class PartitionsNotFoundForGivenTopicException extends HermesException {

  public PartitionsNotFoundForGivenTopicException(String topicName, Throwable cause) {
    super(String.format("Partitions not found for topic: %s", topicName), cause);
  }

  @Override
  public ErrorCode getCode() {
    return ErrorCode.PARTITIONS_NOT_FOUND_FOR_TOPIC;
  }
}
