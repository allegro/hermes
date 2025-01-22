package pl.allegro.tech.hermes.management.api.auth;

import jakarta.ws.rs.container.ContainerRequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.management.config.GroupProperties;

/**
 * Make sure these implementations conform to what is configured via RolesAllowed annotations in
 * endpoints.
 */
@Component
public class ManagementRights {

  private final GroupProperties groupProperties;

  @Autowired
  public ManagementRights(GroupProperties groupProperties) {
    this.groupProperties = groupProperties;
  }

  public boolean isUserAllowedToManageTopic(Topic topic, ContainerRequestContext requestContext) {
    return isAdmin(requestContext)
        || getOwnershipResolver(requestContext).isUserAnOwner(topic.getOwner());
  }

  public boolean isUserAllowedToCreateGroup(ContainerRequestContext requestContext) {
    return isAdmin(requestContext) || groupProperties.isNonAdminCreationEnabled();
  }

  private boolean isUserAllowedToManageGroup(ContainerRequestContext requestContext) {
    return isAdmin(requestContext);
  }

  private boolean isAdmin(ContainerRequestContext requestContext) {
    return requestContext.getSecurityContext().isUserInRole(Roles.ADMIN);
  }

  private SecurityProvider.OwnershipResolver getOwnershipResolver(
      ContainerRequestContext requestContext) {
    return (SecurityProvider.OwnershipResolver)
        requestContext.getProperty(AuthorizationFilter.OWNERSHIP_RESOLVER);
  }

  public CreatorRights<Group> getGroupCreatorRights(ContainerRequestContext requestContext) {
    return new GroupCreatorRights(requestContext);
  }

  class GroupCreatorRights implements CreatorRights<Group> {
    private ContainerRequestContext requestContext;

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
