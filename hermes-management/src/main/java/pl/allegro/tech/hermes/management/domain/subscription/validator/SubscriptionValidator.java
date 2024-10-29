package pl.allegro.tech.hermes.management.domain.subscription.validator;

import java.util.List;
import java.util.Objects;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionPolicy;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionAlreadyExistsException;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.management.api.validator.ApiPreconditions;
import pl.allegro.tech.hermes.management.domain.PermissionDeniedException;
import pl.allegro.tech.hermes.management.domain.auth.RequestUser;
import pl.allegro.tech.hermes.management.domain.owner.validator.OwnerIdValidator;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;

public class SubscriptionValidator {

  private final OwnerIdValidator ownerIdValidator;
  private final ApiPreconditions apiPreconditions;
  private final MessageFilterTypeValidator messageFilterTypeValidator;
  private final TopicService topicService;
  private final SubscriptionRepository subscriptionRepository;
  private final List<EndpointAddressValidator> endpointAddressValidators;
  private final EndpointOwnershipValidator endpointOwnershipValidator;
  private final List<SubscriberWithAccessToAnyTopic> subscribersWithAccessToAnyTopic;

  public SubscriptionValidator(
      OwnerIdValidator ownerIdValidator,
      ApiPreconditions apiPreconditions,
      TopicService topicService,
      SubscriptionRepository subscriptionRepository,
      List<EndpointAddressValidator> endpointAddressValidators,
      EndpointOwnershipValidator endpointOwnershipValidator,
      List<SubscriberWithAccessToAnyTopic> subscribersWithAccessToAnyTopic) {
    this.ownerIdValidator = ownerIdValidator;
    this.apiPreconditions = apiPreconditions;
    this.messageFilterTypeValidator = new MessageFilterTypeValidator();
    this.topicService = topicService;
    this.subscriptionRepository = subscriptionRepository;
    this.endpointAddressValidators = endpointAddressValidators;
    this.endpointOwnershipValidator = endpointOwnershipValidator;
    this.subscribersWithAccessToAnyTopic = subscribersWithAccessToAnyTopic;
  }

  public void checkCreation(Subscription toCheck, RequestUser createdBy) {
    apiPreconditions.checkConstraints(toCheck, createdBy.isAdmin());
    checkOwner(toCheck);
    checkEndpoint(toCheck);
    checkPermissionsToManageSubscription(toCheck, createdBy);
    ensureCreatedSubscriptionInflightIsValid(toCheck, createdBy);
    Topic topic = topicService.getTopicDetails(toCheck.getTopicName());
    checkFilters(toCheck, topic);
    checkIfSubscribingToTopicIsAllowed(toCheck, topic, createdBy);
    if (subscriptionRepository.subscriptionExists(toCheck.getTopicName(), toCheck.getName())) {
      throw new SubscriptionAlreadyExistsException(toCheck);
    }
  }

  public void checkModification(
      Subscription toCheck, RequestUser modifiedBy, Subscription previous) {
    apiPreconditions.checkConstraints(toCheck, modifiedBy.isAdmin());
    checkOwner(toCheck);
    checkEndpoint(toCheck);
    checkPermissionsToManageSubscription(toCheck, modifiedBy);
    ensureUpdatedSubscriptionInflightIsValid(previous, toCheck, modifiedBy);
    Topic topic = topicService.getTopicDetails(toCheck.getTopicName());
    checkFilters(toCheck, topic);
    if (!toCheck.getEndpoint().equals(previous.getEndpoint())) {
      checkIfModifyingEndpointIsAllowed(toCheck, topic, modifiedBy);
    }
    subscriptionRepository.ensureSubscriptionExists(toCheck.getTopicName(), toCheck.getName());
  }

  private void checkOwner(Subscription toCheck) {
    ownerIdValidator.check(toCheck.getOwner());
  }

  private void checkEndpoint(Subscription toCheck) {
    endpointAddressValidators.forEach(validator -> validator.check(toCheck.getEndpoint()));
    endpointOwnershipValidator.check(toCheck.getOwner(), toCheck.getEndpoint());
  }

  private void checkFilters(Subscription toCheck, Topic topic) {
    messageFilterTypeValidator.check(toCheck, topic);
  }

  private void checkIfSubscribingToTopicIsAllowed(
      Subscription toCheck, Topic topic, RequestUser requester) {
    if (isSubscribingForbidden(toCheck, topic, requester)) {
      throw new PermissionDeniedException(
          "Subscribing to this topic has been restricted. Contact the topic owner to create a new subscription.");
    }
  }

  private void checkIfModifyingEndpointIsAllowed(
      Subscription toCheck, Topic topic, RequestUser requester) {
    if (isSubscribingForbidden(toCheck, topic, requester)) {
      throw new PermissionDeniedException(
          "Subscribing to this topic has been restricted. Contact the topic owner to modify the endpoint of this subscription.");
    }
  }

  private boolean isSubscribingForbidden(Subscription toCheck, Topic topic, RequestUser requester) {
    if (topic.isSubscribingRestricted()) {
      boolean privilegedSubscriber =
          subscribersWithAccessToAnyTopic.stream()
              .anyMatch(subscriber -> subscriber.matches(toCheck));
      return !requester.isAdmin() && !requester.isOwner(topic.getOwner()) && !privilegedSubscriber;
    }
    return false;
  }

  private void checkPermissionsToManageSubscription(Subscription toCheck, RequestUser requester) {
    if (!requester.isAdmin() && !requester.isOwner(toCheck.getOwner())) {
      throw new SubscriptionValidationException(
          "Provide an owner that includes you, you would not be able to manage this subscription later");
    }
  }

  private void ensureCreatedSubscriptionInflightIsValid(
      Subscription subscription, RequestUser requester) {
    if (requester.isAdmin()) {
      return;
    }
    SubscriptionPolicy subscriptionPolicy = subscription.getSerialSubscriptionPolicy();
    if (subscriptionPolicy == null) {
      return;
    }
    if (subscriptionPolicy.getInflightSize() != null) {
      throw new SubscriptionValidationException("Inflight size can't be set by non admin users");
    }
  }

  private void ensureUpdatedSubscriptionInflightIsValid(
      Subscription previous, Subscription updated, RequestUser requester) {
    if (requester.isAdmin()) {
      return;
    }

    SubscriptionPolicy updatedSubscriptionPolicy = updated.getSerialSubscriptionPolicy();
    if (updatedSubscriptionPolicy == null) {
      return;
    }
    Integer updatedInflight = updatedSubscriptionPolicy.getInflightSize();

    SubscriptionPolicy previousSubscriptionPolicy = previous.getSerialSubscriptionPolicy();
    Integer previousInflight =
        previousSubscriptionPolicy == null ? null : previousSubscriptionPolicy.getInflightSize();

    if (!Objects.equals(previousInflight, updatedInflight)) {
      throw new SubscriptionValidationException(
          String.format(
              "Inflight size can't be changed by non admin users. Changed from: %s, to: %s",
              previousInflight, updatedInflight));
    }
  }
}
