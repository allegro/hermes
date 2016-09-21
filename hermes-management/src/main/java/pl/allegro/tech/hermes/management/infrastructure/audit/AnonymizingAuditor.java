package pl.allegro.tech.hermes.management.infrastructure.audit;

import pl.allegro.tech.hermes.api.Anonymizable;
import pl.allegro.tech.hermes.management.domain.Auditor;

import static com.google.common.base.Preconditions.checkNotNull;

public class AnonymizingAuditor implements Auditor<Anonymizable> {

    private Auditor auditor;

    public AnonymizingAuditor(Auditor auditor) {
        this.auditor = checkNotNull(auditor);
    }

    @Override
    public void objectCreated(String username, Anonymizable createdObject) {
        auditor.objectCreated(username, createdObject.anonymize());
    }

    @Override
    public void objectUpdated(String username, Anonymizable oldObject, Anonymizable newObject) {
        auditor.objectUpdated(username, oldObject.anonymize(), newObject.anonymize());
    }
}
