package pl.allegro.tech.hermes.management.api.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;

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
        return requestContext.getSecurityContext().isUserInRole(Roles.ADMIN) ||
                getOwnershipResolver(requestContext).isUserAnOwner(topic.getOwner());
    }

    public boolean isUserAllowedToManageSubscription(Subscription subscription, ContainerRequestContext requestContext) {
        return requestContext.getSecurityContext().isUserInRole(Roles.ADMIN) ||
                getOwnershipResolver(requestContext).isUserAnOwner(topicRepository.getTopicDetails(subscription.getTopicName()).getOwner()) ||
                getOwnershipResolver(requestContext).isUserAnOwner(subscription.getOwner());
    }

    private SecurityProvider.OwnershipResolver getOwnershipResolver(ContainerRequestContext requestContext) {
        return (SecurityProvider.OwnershipResolver) requestContext.getProperty(AuthorizationFilter.OWNERSHIP_RESOLVER);
    }

}
