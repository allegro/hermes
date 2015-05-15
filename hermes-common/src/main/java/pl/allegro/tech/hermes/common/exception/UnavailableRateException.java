package pl.allegro.tech.hermes.common.exception;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.TopicName;

@SuppressWarnings("serial")
public class UnavailableRateException extends HermesException {

    public UnavailableRateException(TopicName topicName, String subscriptionName, Throwable cause) {
        super(String.format(
                "Rate for %s subscription on %s topic in group %s is unavailable.",
                subscriptionName, topicName.getName(), topicName.getGroupName()), cause
            );
    }

    public UnavailableRateException(TopicName topicName, Throwable cause) {
        super(String.format(
                "Rate for %s topic in group %s is unavailable",
                topicName.getName(), topicName.getGroupName()),
                cause
            );
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.UNAVAILABLE_RATE;
    }
}
