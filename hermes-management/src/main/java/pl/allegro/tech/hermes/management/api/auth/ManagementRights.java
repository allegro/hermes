package pl.allegro.tech.hermes.management.api.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.management.config.GroupProperties;

import javax.ws.rs.container.ContainerRequestContext;

/**
 * Make sure these implementations conform to what is configured via RolesAllowed annotations in endpoints.
 */
@Component
public class ManagementRights {

    private final TopicRepository topicRepository;
    private final GroupProperties groupProperties;

    @Autowired
    public ManagementRights(TopicRepository topicRepository, GroupProperties groupProperties) {
        this.topicRepository = topicRepository;
        this.groupProperties = groupProperties;
    }

    public boolean isUserAllowedToManageTopic(Topic topic, ContainerRequestContext requestContext) {
        return isAdmin(requestContext) ||
                getOwnershipResolver(requestContext).isUserAnOwner(topic.getOwner());
    }

    public boolean isUserAllowedToCreateSubscription(Subscription subscription, ContainerRequestContext requestContext) {
        return !topicRepository.isSubscribingRestricted(subscription.getTopicName()) || isAdmin(requestContext);
    }

    public boolean isUserAllowedToCreateGroup(ContainerRequestContext requestContext) {
        return isAdmin(requestContext) || groupProperties.isNonAdminCreationEnabled();
    }

    private boolean isUserAllowedToManageGroup(ContainerRequestContext requestContext) {
        return isAdmin(requestContext);
    }

    public boolean isUserAllowedToManageSubscription(Subscription subscription, ContainerRequestContext requestContext) {
        return isAdmin(requestContext) || isSubscriptionOwner(subscription, requestContext);
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

    public CreatorRights<Subscription> getSubscriptionCreatorRights(ContainerRequestContext requestContext) {
        return new SubscriptionCreatorRights(requestContext);
    }

    public CreatorRights<Group> getGroupCreatorRights(ContainerRequestContext requestContext) {
        return new GroupCreatorRights(requestContext);
    }

    class SubscriptionCreatorRights implements CreatorRights<Subscription> {
        private final ContainerRequestContext requestContext;

        SubscriptionCreatorRights(ContainerRequestContext requestContext) {
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

    class GroupCreatorRights implements CreatorRights<Group> {
        private final ContainerRequestContext requestContext;

        GroupCreatorRights(ContainerRequestContext requestContext) {
            this.requestContext = requestContext;
        }

        @Override
        public boolean allowedToManage(Group group) {
            return ManagementRights.this.isUserAllowedToManageGroup(requestContext);
        }

        @Override
        public boolean allowedToCreate(Group group) {
            return ManagementRights.this.isUserAllowedToCreateGroup(requestContext);
        }
    }
}
