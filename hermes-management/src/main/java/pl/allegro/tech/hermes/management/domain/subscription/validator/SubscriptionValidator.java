package pl.allegro.tech.hermes.management.domain.subscription.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.management.api.validator.ApiPreconditions;
import pl.allegro.tech.hermes.management.domain.PermissionDeniedException;
import pl.allegro.tech.hermes.management.domain.owner.validator.OwnerIdValidator;
import pl.allegro.tech.hermes.management.domain.subscription.CreatorRights;

@Component
public class SubscriptionValidator {

    private final OwnerIdValidator ownerIdValidator;
    private final ApiPreconditions apiPreconditions;
    private final MessageFilterTypeValidator messageFilterTypeValidator;

    @Autowired
    public SubscriptionValidator(OwnerIdValidator ownerIdValidator,
                                 ApiPreconditions apiPreconditions,
                                 MessageFilterTypeValidator messageFilterTypeValidator) {
        this.ownerIdValidator = ownerIdValidator;
        this.apiPreconditions = apiPreconditions;
        this.messageFilterTypeValidator = messageFilterTypeValidator;
    }

    public void checkCreation(Subscription toCheck, Topic topic, CreatorRights creatorRights) {
        apiPreconditions.checkConstraints(toCheck);
        ownerIdValidator.check(toCheck.getOwner());
        messageFilterTypeValidator.check(toCheck, topic);

        if (!creatorRights.allowedToCreate(toCheck)) {
            throw new PermissionDeniedException("You are not allowed to create subscriptions for this topic.");
        }
        if (!creatorRights.allowedToManage(toCheck)) {
            throw new SubscriptionValidationException("Provide an owner that includes you, you would not be able to manage this subscription later");
        }
    }

    public void checkModification(Subscription toCheck, Topic topic) {
        apiPreconditions.checkConstraints(toCheck);
        ownerIdValidator.check(toCheck.getOwner());
        messageFilterTypeValidator.check(toCheck, topic);
    }

}
