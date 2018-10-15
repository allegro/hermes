package pl.allegro.tech.hermes.api;

public enum TrackingMode {

    TRACK_ALL("trackingAll"),
    TRACK_DISCARDED_ONLY("discardedOnly"),
    TRACKING_OFF("trackingOff");

    private String value;

    TrackingMode(String s) {
        this.value = s;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    public static TrackingMode fromString(String trackingMode) {

        if(trackingMode == null) {
            return null;
        }

        switch(trackingMode){
            case "trackingAll":
                return TRACK_ALL;
            case "discardedOnly":
                return TRACK_DISCARDED_ONLY;
            case "trackingOff":
                return TRACKING_OFF;
            default:
                return TRACKING_OFF;
        }

    }
}
