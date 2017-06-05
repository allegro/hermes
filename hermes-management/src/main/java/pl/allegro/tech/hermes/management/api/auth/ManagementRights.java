package pl.allegro.tech.hermes.management.api.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.management.domain.subscription.CreatorRights;

import javax.ws.rs.container.ContainerRequestContext;

/**
 * Make sure these implementations conform to what is configured via RolesAllowed annotations in endpoints.
 */
@Component
public class ManagementRights {

    private final TopicRepository topicRepository;

    @Autowired
    public ManagementRights(TopicRepository topicRepository) {
        this.topicRepository = topicRepository;
    }

    public boolean isUserAllowedToManageTopic(Topic topic, ContainerRequestContext requestContext) {
        return isAdmin(requestContext) ||
                getOwnershipResolver(requestContext).isUserAnOwner(topic.getOwner());
    }

    public boolean isUserAllowedToCreateSubscription(Subscription subscription, ContainerRequestContext requestContext) {
        return !topicRepository.isSubscribingRestricted(subscription.getTopicName()) ||
                isAdmin(requestContext) || isTopicOwner(subscription, requestContext);
    }

    public boolean isUserAllowedToManageSubscription(Subscription subscription, ContainerRequestContext requestContext) {
        return isAdmin(requestContext) || isTopicOwner(subscription, requestContext) ||
                isSubscriptionOwner(subscription, requestContext);
    }

    private boolean isTopicOwner(Subscription subscription, ContainerRequestContext requestContext) {
        return getOwnershipResolver(requestContext).isUserAnOwner(topicRepository.getTopicDetails(subscription.getTopicName()).getOwner());
    }

    private boolean isSubscriptionOwner(Subscription subscription, ContainerRequestContext requestContext) {
        return getOwnershipResolver(requestContext).isUserAnOwner(subscription.getOwner());
    }

    private boolean isAdmin(ContainerRequestContext requestContext) {
        return requestContext.getSecurityContext().isUserInRole(Roles.ADMIN);
    }

    private SecurityProvider.OwnershipResolver getOwnershipResolver(ContainerRequestContext requestContext) {
        return (SecurityProvider.OwnershipResolver) requestContext.getProperty(AuthorizationFilter.OWNERSHIP_RESOLVER);
    }

    public CreatorRights getSubscriptionCreatorRights(ContainerRequestContext requestContext) {
        return new SubscriptionCreatorRights(requestContext);
    }

    private class SubscriptionCreatorRights implements CreatorRights {
        private ContainerRequestContext requestContext;

        private SubscriptionCreatorRights(ContainerRequestContext requestContext) {
            this.requestContext = requestContext;
        }

        @Override
        public boolean allowedToManage(Subscription subscription) {
            return ManagementRights.this.isUserAllowedToManageSubscription(subscription, requestContext);
        }

        @Override
        public boolean allowedToCreate(Subscription subscription) {
            return ManagementRights.this.isUserAllowedToCreateSubscription(subscription, requestContext);
        }
    }
}
