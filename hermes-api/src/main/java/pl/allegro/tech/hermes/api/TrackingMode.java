package pl.allegro.tech.hermes.api;

import java.util.Optional;

public enum TrackingMode {
  TRACK_ALL("trackingAll"),
  TRACK_DISCARDED_ONLY("discardedOnly"),
  TRACKING_OFF("trackingOff");

  private String value;

  TrackingMode(String s) {
    this.value = s;
  }

  public static Optional<TrackingMode> fromString(String trackingMode) {

    if (trackingMode == null) {
      return Optional.empty();
    }

    switch (trackingMode) {
      case "trackingAll":
        return Optional.of(TRACK_ALL);
      case "discardedOnly":
        return Optional.of(TRACK_DISCARDED_ONLY);
      case "trackingOff":
      default:
        return Optional.of(TRACKING_OFF);
    }
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return this.value;
  }
}
