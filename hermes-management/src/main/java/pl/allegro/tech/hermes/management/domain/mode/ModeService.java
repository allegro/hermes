package pl.allegro.tech.hermes.management.domain.mode;

import org.springframework.stereotype.Component;

@Component
public class ModeService {

    public static final String READ_WRITE = "readWrite";
    public static final String READ_ONLY = "readOnly";

    public enum ManagementMode {
        READ_WRITE(ModeService.READ_WRITE),
        READ_ONLY(ModeService.READ_ONLY);

        private final String text;

        ManagementMode(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    private volatile ManagementMode mode = ManagementMode.READ_WRITE;

    public ManagementMode getMode() {
        return mode;
    }

    public void setMode(ManagementMode mode) {
        this.mode = mode;
    }

    public boolean isReadOnlyEnabled() {
        return mode == ManagementMode.READ_ONLY;
    }
}
