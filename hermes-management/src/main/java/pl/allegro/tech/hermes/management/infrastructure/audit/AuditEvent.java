package pl.allegro.tech.hermes.management.infrastructure.audit;

public class AuditEvent {
  private final AuditEventType eventType;
  private final String resourceName;
  private final String payloadClass;
  private final String payload;
  private final String username;

  public AuditEvent(
      AuditEventType eventType,
      String payload,
      String payloadClass,
      String resourceName,
      String username) {
    this.eventType = eventType;
    this.payload = payload;
    this.payloadClass = payloadClass;
    this.resourceName = resourceName;
    this.username = username;
  }

  public AuditEventType getEventType() {
    return eventType;
  }

  public String getPayloadClass() {
    return payloadClass;
  }

  public String getPayload() {
    return payload;
  }

  public String getUsername() {
    return username;
  }

  public String getResourceName() {
    return resourceName;
  }
}
