package pl.allegro.tech.hermes.management.domain.subscription.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionAlreadyExistsException;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.management.api.auth.CreatorRights;
import pl.allegro.tech.hermes.management.api.validator.ApiPreconditions;
import pl.allegro.tech.hermes.management.domain.PermissionDeniedException;
import pl.allegro.tech.hermes.management.domain.owner.validator.OwnerIdValidator;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;

import java.util.List;

@Component
public class SubscriptionValidator {

    private final OwnerIdValidator ownerIdValidator;
    private final ApiPreconditions apiPreconditions;
    private final MessageFilterTypeValidator messageFilterTypeValidator;
    private final TopicService topicService;
    private final SubscriptionRepository subscriptionRepository;
    private final EndpointOwnershipValidator endpointOwnershipValidator;
    private final List<EndpointAddressValidator> endpointAddressValidators;

    @Autowired
    public SubscriptionValidator(OwnerIdValidator ownerIdValidator,
                                 ApiPreconditions apiPreconditions,
                                 MessageFilterTypeValidator messageFilterTypeValidator,
                                 TopicService topicService,
                                 SubscriptionRepository subscriptionRepository,
                                 EndpointOwnershipValidator endpointOwnershipValidator,
                                 List<EndpointAddressValidator> endpointAddressValidators) {
        this.ownerIdValidator = ownerIdValidator;
        this.apiPreconditions = apiPreconditions;
        this.messageFilterTypeValidator = messageFilterTypeValidator;
        this.topicService = topicService;
        this.subscriptionRepository = subscriptionRepository;
        this.endpointOwnershipValidator = endpointOwnershipValidator;
        this.endpointAddressValidators = endpointAddressValidators;
    }

    public void checkCreation(Subscription toCheck, CreatorRights<Subscription> creatorRights) {
        checkWhileCreationOrModification(toCheck);

        if (!creatorRights.allowedToCreate(toCheck)) {
            throw new PermissionDeniedException("You are not allowed to create subscriptions for this topic.");
        }
        if (!creatorRights.allowedToManage(toCheck)) {
            throw new SubscriptionValidationException("Provide an owner that includes you, you would not be able to manage this subscription later");
        }
        if (subscriptionRepository.subscriptionExists(toCheck.getTopicName(), toCheck.getName())) {
            throw new SubscriptionAlreadyExistsException(toCheck);
        }
    }

    public void checkModification(Subscription toCheck) {
        checkWhileCreationOrModification(toCheck);
        subscriptionRepository.ensureSubscriptionExists(toCheck.getTopicName(), toCheck.getName());
    }

    private void checkWhileCreationOrModification(Subscription toCheck) {
        apiPreconditions.checkConstraints(toCheck, false);
        ownerIdValidator.check(toCheck.getOwner());
        endpointAddressValidators.forEach(validator -> validator.check(toCheck.getEndpoint()));
        endpointOwnershipValidator.check(toCheck.getOwner(), toCheck.getEndpoint());
        messageFilterTypeValidator.check(toCheck, topicService.getTopicDetails(toCheck.getTopicName()));
    }
}
