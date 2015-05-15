package pl.allegro.tech.hermes.domain.subscription;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.exception.HermesException;

public class SubscriptionAlreadyExistsException extends HermesException {

    public SubscriptionAlreadyExistsException(Subscription subscription, Throwable cause) {
        super(String.format("Subscription %s for topic %s does not exist.",
                subscription.getName(), subscription.getQualifiedTopicName()), cause);
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.SUBSCRIPTION_ALREADY_EXISTS;
    }
}
