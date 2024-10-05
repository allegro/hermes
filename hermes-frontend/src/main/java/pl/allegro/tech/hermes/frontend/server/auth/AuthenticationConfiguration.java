package pl.allegro.tech.hermes.frontend.server.auth;

import com.google.common.base.Preconditions;
import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.idm.IdentityManager;
import io.undertow.server.HttpServerExchange;
import java.util.List;
import java.util.function.Predicate;

public class AuthenticationConfiguration {

  private final Predicate<HttpServerExchange> isAuthenticationRequiredPredicate;
  private final List<AuthenticationMechanism> authMechanisms;
  private final IdentityManager identityManager;

  public AuthenticationConfiguration(
      Predicate<HttpServerExchange> isAuthenticationRequiredPredicate,
      List<AuthenticationMechanism> authMechanisms,
      IdentityManager identityManager) {
    Preconditions.checkNotNull(
        isAuthenticationRequiredPredicate, "IsAuthenticationRequired predicate has to be provided");
    Preconditions.checkArgument(
        !authMechanisms.isEmpty(), "At least one AuthenticationMechanism has to be provided.");
    Preconditions.checkNotNull(identityManager, "IdentityManager has to be provided");

    this.isAuthenticationRequiredPredicate = isAuthenticationRequiredPredicate;
    this.authMechanisms = authMechanisms;
    this.identityManager = identityManager;
  }

  public Predicate<HttpServerExchange> getIsAuthenticationRequiredPredicate() {
    return isAuthenticationRequiredPredicate;
  }

  public List<AuthenticationMechanism> getAuthMechanisms() {
    return authMechanisms;
  }

  public IdentityManager getIdentityManager() {
    return identityManager;
  }
}
