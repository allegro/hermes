package pl.allegro.tech.hermes.management.domain.owner;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.Owner;
import pl.allegro.tech.hermes.management.domain.ManagementException;

import java.util.List;
import java.util.Optional;

public interface OwnerSource {

    String name();

    boolean exists(String ownerId);

    Owner get(String id) throws OwnerNotFound;

    default boolean isDeprecated() {
        return false;
    }

    /**
     * Override if the implemented owner source supports autocompletion.
     */
    default Optional<Autocompletion> autocompletion() {
        return Optional.empty();
    }

    interface Autocompletion {
        List<Owner> ownersMatching(String searchString);
    }

    class OwnerNotFound extends ManagementException {

        public OwnerNotFound(String source, String id) {
            super("Owner of id '" + id + "' not found in source " + source);
        }

        @Override
        public ErrorCode getCode() {
            return ErrorCode.OWNER_NOT_FOUND;
        }
    }

}
