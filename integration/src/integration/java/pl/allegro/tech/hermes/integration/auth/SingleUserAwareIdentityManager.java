package pl.allegro.tech.hermes.integration.auth;

import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;
import org.apache.commons.codec.binary.Base64;
import pl.allegro.tech.hermes.frontend.server.auth.Roles;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

    private final class SomeUserAccount implements Account {

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

    private final class SomeUserPrincipal implements Principal {

        private final String username;

        private SomeUserPrincipal(String username) {
            this.username = username;
        }

        @Override
        public String getName() {
            return username;
        }
    }

    static Map<String, String> getHeadersWithAuthentication(String username, String password) {
        String credentials = username + ":" + password;
        String token = "Basic " + Base64.encodeBase64String(credentials.getBytes(StandardCharsets.UTF_8));

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", token);
        return headers;
    }
}