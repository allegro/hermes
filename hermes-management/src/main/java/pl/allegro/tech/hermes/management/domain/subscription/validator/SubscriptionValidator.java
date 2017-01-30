package pl.allegro.tech.hermes.management.domain.subscription.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.management.api.validator.ApiPreconditions;
import pl.allegro.tech.hermes.management.domain.owner.validator.OwnerIdValidator;

@Component
public class SubscriptionValidator {

    private final OwnerIdValidator ownerIdValidator;
    private final ApiPreconditions apiPreconditions;

    @Autowired
    public SubscriptionValidator(OwnerIdValidator ownerIdValidator,
                                 ApiPreconditions apiPreconditions) {
        this.ownerIdValidator = ownerIdValidator;
        this.apiPreconditions = apiPreconditions;
    }

    public void check(Subscription toCheck) {
        apiPreconditions.checkConstraints(toCheck);
        ownerIdValidator.check(toCheck.getOwner());
    }

}
