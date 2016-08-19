package pl.allegro.tech.hermes.management.domain.topic.blacklist;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.common.exception.HermesException;

import static pl.allegro.tech.hermes.api.ErrorCode.TOPIC_NOT_BLACKLISTED;

public class NotBlacklistedException extends HermesException {

    @Override
    public ErrorCode getCode() {
        return TOPIC_NOT_BLACKLISTED;
    }

    public NotBlacklistedException(String qualifiedTopicName, Throwable cause) {
        super("Topic not blacklisted: " + qualifiedTopicName, cause);
    }

    public NotBlacklistedException(String qualifiedTopicName) {
        this(qualifiedTopicName, null);
    }
}
