package pl.allegro.tech.hermes.management.domain.blacklist;

import static pl.allegro.tech.hermes.api.ErrorCode.TOPIC_NOT_UNBLACKLISTED;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.common.exception.HermesException;

public class NotUnblacklistedException extends HermesException {

  @Override
  public ErrorCode getCode() {
    return TOPIC_NOT_UNBLACKLISTED;
  }

  public NotUnblacklistedException(String qualifiedTopicName, Throwable cause) {
    super("Topic not unblacklisted: " + qualifiedTopicName, cause);
  }

  public NotUnblacklistedException(String qualifiedTopicName) {
    this(qualifiedTopicName, null);
  }
}
