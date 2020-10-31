package pl.allegro.tech.hermes.management.infrastructure.audit.pojo;

public class AuditEvent {
    private final AuditEventType eventType;
    private final Class<?> resourceClass;
    private final Class<?> payloadClass;
    private final Object payload;
    private final String username;

    public AuditEvent(AuditEventType eventType, Object payload, String username) {
        this(eventType, payload, payload.getClass(), username);
    }

    public AuditEvent(AuditEventType eventType, Object payload, Class<?> resourceClass, String username){
        this.eventType = eventType;
        this.payload = payload;
        this.payloadClass = payload.getClass();
        this.resourceClass = resourceClass;
        this.username = username;
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

    public String getUsername() {
        return username;
    }

    public Class<?> getResourceClass() {
        return resourceClass;
    }
}
