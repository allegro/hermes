package pl.allegro.tech.hermes.management.domain.subscription.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.management.domain.maintainer.validator.MaintainerDescriptorValidator;

@Component
public class SubscriptionValidator {

    private final MaintainerDescriptorValidator maintainerDescriptorValidator;

    @Autowired
    public SubscriptionValidator(MaintainerDescriptorValidator maintainerDescriptorValidator) {
        this.maintainerDescriptorValidator = maintainerDescriptorValidator;
    }

    public void check(Subscription toCheck) {
        maintainerDescriptorValidator.check(toCheck.getMaintainer());
    }

}
