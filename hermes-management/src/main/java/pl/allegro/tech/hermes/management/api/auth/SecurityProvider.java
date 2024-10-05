package pl.allegro.tech.hermes.management.api.auth;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.SecurityContext;
import pl.allegro.tech.hermes.api.OwnerId;

public interface SecurityProvider {

  HermesSecurity security(ContainerRequestContext requestContext);

  class HermesSecurity {
    private final SecurityContext securityContext;
    private final OwnershipResolver ownershipResolver;

    public HermesSecurity(SecurityContext securityContext, OwnershipResolver ownershipResolver) {
      this.securityContext = securityContext;
      this.ownershipResolver = ownershipResolver;
    }

    public SecurityContext getSecurityContext() {
      return securityContext;
    }

    public OwnershipResolver getOwnershipResolver() {
      return ownershipResolver;
    }
  }

  interface OwnershipResolver {
    boolean isUserAnOwner(OwnerId owner);
  }
}
