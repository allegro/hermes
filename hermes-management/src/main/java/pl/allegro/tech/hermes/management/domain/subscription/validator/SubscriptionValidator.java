package pl.allegro.tech.hermes.management.domain.subscription.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.management.api.validator.ApiPreconditions;
import pl.allegro.tech.hermes.management.domain.PermissionDeniedException;
import pl.allegro.tech.hermes.management.domain.owner.validator.OwnerIdValidator;
import pl.allegro.tech.hermes.management.domain.subscription.CreatorRights;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;

@Component
public class SubscriptionValidator {

    private final OwnerIdValidator ownerIdValidator;
    private final ApiPreconditions apiPreconditions;
    private final MessageFilterTypeValidator messageFilterTypeValidator;
    private final TopicService topicService;

    @Autowired
    public SubscriptionValidator(OwnerIdValidator ownerIdValidator,
                                 ApiPreconditions apiPreconditions,
                                 MessageFilterTypeValidator messageFilterTypeValidator,
                                 TopicService topicService) {
        this.ownerIdValidator = ownerIdValidator;
        this.apiPreconditions = apiPreconditions;
        this.messageFilterTypeValidator = messageFilterTypeValidator;
        this.topicService = topicService;
    }

    public void checkCreation(Subscription toCheck, CreatorRights creatorRights) {
        apiPreconditions.checkConstraints(toCheck);
        ownerIdValidator.check(toCheck.getOwner());
        messageFilterTypeValidator.check(toCheck, topicService.getTopicDetails(toCheck.getTopicName()));

        if (!creatorRights.allowedToCreate(toCheck)) {
            throw new PermissionDeniedException("You are not allowed to create subscriptions for this topic.");
        }
        if (!creatorRights.allowedToManage(toCheck)) {
            throw new SubscriptionValidationException("Provide an owner that includes you, you would not be able to manage this subscription later");
        }
    }

    public void checkModification(Subscription toCheck) {
        apiPreconditions.checkConstraints(toCheck);
        ownerIdValidator.check(toCheck.getOwner());
        messageFilterTypeValidator.check(toCheck, topicService.getTopicDetails(toCheck.getTopicName()));
    }

}
