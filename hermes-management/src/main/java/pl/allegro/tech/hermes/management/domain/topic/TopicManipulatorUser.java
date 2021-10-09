package pl.allegro.tech.hermes.management.domain.topic;

import pl.allegro.tech.hermes.management.api.auth.Roles;

import javax.ws.rs.core.SecurityContext;
import java.util.Objects;

public class TopicManipulatorUser {
    private final String username;
    private final boolean isAdmin;

    public TopicManipulatorUser(String username, boolean isAdmin) {
        this.username = username;
        this.isAdmin = isAdmin;
    }

    public static TopicManipulatorUser fromSecurityContext(SecurityContext securityContext) {
        String username = securityContext.getUserPrincipal().getName();
        boolean isAdmin = securityContext.isUserInRole(Roles.ADMIN);
        return new TopicManipulatorUser(username, isAdmin);
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
        TopicManipulatorUser topicManipulatorUser = (TopicManipulatorUser) o;
        return isAdmin == topicManipulatorUser.isAdmin && Objects.equals(username, topicManipulatorUser.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, isAdmin);
    }
}
