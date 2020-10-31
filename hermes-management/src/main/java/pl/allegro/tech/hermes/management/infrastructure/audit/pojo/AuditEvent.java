package pl.allegro.tech.hermes.management.infrastructure.audit.pojo;

public class AuditEvent {
    private final AuditEventType eventType;
    private final String resourceName;
    private final Class<?> payloadClass;
    private final Object payload;
    private final String username;

    public AuditEvent(AuditEventType eventType, Object payload, String username) {
        this(eventType, payload, payload.getClass().getSimpleName(), username);
    }

    public AuditEvent(AuditEventType eventType, Object payload, String resourceName, String username){
        this.eventType = eventType;
        this.payload = payload;
        this.payloadClass = payload.getClass();
        this.resourceName = resourceName;
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

    public String getResourceName() {
        return resourceName;
    }
}
