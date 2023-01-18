package pl.allegro.tech.hermes.management.domain.auth

import pl.allegro.tech.hermes.api.OwnerId

class TestRequestUser implements RequestUser {

    private final String username
    private final boolean admin
    private final boolean isOwner

    TestRequestUser(String username, boolean admin) {
        this(username, admin, false)
    }

    TestRequestUser(String username, boolean admin, boolean isOwner) {
        this.username = username
        this.admin = admin
        this.isOwner = isOwner
    }

    @Override
    String getUsername() {
        return username
    }

    @Override
    boolean isAdmin() {
        return admin
    }

    @Override
    boolean isOwner(OwnerId ownerId) {
        return isOwner
    }
}
