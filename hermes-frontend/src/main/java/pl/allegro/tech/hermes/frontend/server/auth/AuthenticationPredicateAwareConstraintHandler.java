package pl.allegro.tech.hermes.frontend.server.auth;

import io.undertow.security.handlers.AuthenticationConstraintHandler;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import java.util.function.Predicate;

public class AuthenticationPredicateAwareConstraintHandler extends AuthenticationConstraintHandler {

  private final Predicate<HttpServerExchange> predicate;

  public AuthenticationPredicateAwareConstraintHandler(
      HttpHandler next, Predicate<HttpServerExchange> predicate) {
    super(next);
    this.predicate = predicate;
  }

  @Override
  protected boolean isAuthenticationRequired(HttpServerExchange exchange) {
    return predicate.test(exchange);
  }
}
