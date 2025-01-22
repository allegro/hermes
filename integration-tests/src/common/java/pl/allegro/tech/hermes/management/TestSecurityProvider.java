package pl.allegro.tech.hermes.management;

import static java.util.stream.Collectors.toSet;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.NotImplementedException;
import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.management.api.auth.SecurityProvider;

public class TestSecurityProvider implements SecurityProvider {

  private static volatile boolean userIsAdmin = true;
  private static final Set<OwnerId> ownerIds = ConcurrentHashMap.newKeySet();

  public static void setUserIsAdmin(boolean userIsAdmin) {
    TestSecurityProvider.userIsAdmin = userIsAdmin;
  }

  public static void setUserAsOwner(OwnerId... ownerIds) {
    TestSecurityProvider.ownerIds.addAll(Arrays.stream(ownerIds).collect(toSet()));
  }

  public static void reset() {
    userIsAdmin = true;
    ownerIds.clear();
  }

  @Override
  public HermesSecurity security(ContainerRequestContext requestContext) {
    return new HermesSecurity(securityContext(), ownerIds::contains);
  }

  private SecurityContext securityContext() {

    return new SecurityContext() {
      @Override
      public Principal getUserPrincipal() {
        return new TestUserPrincipal();
      }

      @Override
      public boolean isUserInRole(String role) {
        return checkRoles(role);
      }

      @Override
      public boolean isSecure() {
        throw new NotImplementedException();
      }

      @Override
      public String getAuthenticationScheme() {
        throw new NotImplementedException();
      }
    };
  }

  private boolean checkRoles(String role) {
    if (role.equalsIgnoreCase("admin")) {
      return userIsAdmin;
    } else {
      return true;
    }
  }

  private static class TestUserPrincipal implements Principal {

    @Override
    public String getName() {
      return "test-user";
    }
  }
}
