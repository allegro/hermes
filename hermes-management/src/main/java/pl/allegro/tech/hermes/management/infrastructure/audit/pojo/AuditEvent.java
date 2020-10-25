package pl.allegro.tech.hermes.management.infrastructure.audit.pojo;

public class AuditEvent {
    private final AuditEventType eventType;
    private final Class<?> payloadClass;
    private final Object payload;

    public AuditEvent(AuditEventType eventType, Object payload) {
        this.eventType = eventType;
        this.payload = payload;
        this.payloadClass = payload.getClass();
    }

    public AuditEventType getEventType() {
        return eventType;
    }

    public Class<?> getPayloadClass() {
        return payloadClass;
    }

    public Object getPayload() {
        return payload;
    }
}
