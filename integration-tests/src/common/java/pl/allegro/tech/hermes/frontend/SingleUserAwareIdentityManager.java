package pl.allegro.tech.hermes.frontend;

import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;
import java.security.Principal;
import java.util.Collections;
import java.util.Set;
import pl.allegro.tech.hermes.frontend.server.auth.Roles;

public class SingleUserAwareIdentityManager implements IdentityManager {

  private final String username;
  private final String password;

  public SingleUserAwareIdentityManager(String username, String password) {
    this.username = username;
    this.password = password;
  }

  @Override
  public Account verify(Account account) {
    return null;
  }

  @Override
  public Account verify(String username, Credential credential) {
    String password = new String(((PasswordCredential) credential).getPassword());
    if (this.username.equals(username) && this.password.equals(password)) {
      return new SomeUserAccount(username);
    }
    return null;
  }

  @Override
  public Account verify(Credential credential) {
    return null;
  }

  private static final class SomeUserAccount implements Account {

    private final Principal principal;

    private SomeUserAccount(String username) {
      this.principal = new SomeUserPrincipal(username);
    }

    @Override
    public Principal getPrincipal() {
      return principal;
    }

    @Override
    public Set<String> getRoles() {
      return Collections.singleton(Roles.PUBLISHER);
    }
  }

  private record SomeUserPrincipal(String username) implements Principal {

    @Override
    public String getName() {
      return username;
    }
  }
}
