package pl.allegro.tech.hermes.management.domain.auth;

import pl.allegro.tech.hermes.management.api.auth.Roles;

import javax.ws.rs.core.SecurityContext;
import java.util.Objects;

public class RequestUser {
    private final String username;
    private final boolean isAdmin;

    public RequestUser(String username, boolean isAdmin) {
        this.username = username;
        this.isAdmin = isAdmin;
    }

    public static RequestUser fromSecurityContext(SecurityContext securityContext) {
        String username = securityContext.getUserPrincipal().getName();
        boolean isAdmin = securityContext.isUserInRole(Roles.ADMIN);
        return new RequestUser(username, isAdmin);
    }

    public String getUsername() {
        return username;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequestUser requestUser = (RequestUser) o;
        return isAdmin == requestUser.isAdmin && Objects.equals(username, requestUser.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, isAdmin);
    }
}
