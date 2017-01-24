package pl.allegro.tech.hermes.management.domain.subscription.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.management.api.validator.ApiPreconditions;
import pl.allegro.tech.hermes.management.domain.maintainer.validator.MaintainerDescriptorValidator;

@Component
public class SubscriptionValidator {

    private final MaintainerDescriptorValidator maintainerDescriptorValidator;
    private final ApiPreconditions apiPreconditions;

    @Autowired
    public SubscriptionValidator(MaintainerDescriptorValidator maintainerDescriptorValidator,
                                 ApiPreconditions apiPreconditions) {
        this.maintainerDescriptorValidator = maintainerDescriptorValidator;
        this.apiPreconditions = apiPreconditions;
    }

    public void check(Subscription toCheck) {
        apiPreconditions.checkConstraints(toCheck);
        maintainerDescriptorValidator.check(toCheck.getMaintainer());
    }

}
