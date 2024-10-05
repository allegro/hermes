package pl.allegro.tech.hermes.management.domain.mode;

import org.springframework.stereotype.Component;

@Component
public class ModeService {

  public static final String READ_WRITE = "readWrite";
  public static final String READ_ONLY = "readOnly";
  public static final String READ_ONLY_ADMIN = "readOnlyAdmin";

  public enum ManagementMode {
    READ_WRITE(ModeService.READ_WRITE),
    READ_ONLY(ModeService.READ_ONLY),
    READ_ONLY_ADMIN(ModeService.READ_ONLY_ADMIN);

    private final String text;

    ManagementMode(String text) {
      this.text = text;
    }

    @Override
    public String toString() {
      return text;
    }
  }

  private volatile ManagementMode mode = ManagementMode.READ_ONLY;

  public ManagementMode getMode() {
    return mode;
  }

  public synchronized void setModeByAdmin(ManagementMode mode) {
    this.mode = mode;
  }

  public boolean isReadOnlyEnabled() {
    return mode == ManagementMode.READ_ONLY || mode == ManagementMode.READ_ONLY_ADMIN;
  }

  public synchronized void setMode(ManagementMode newMode) {
    /* READ_ONLY_ADMIN is a flag that can be changed only by admin */
    if (!mode.equals(ManagementMode.READ_ONLY_ADMIN)) {
      this.mode = newMode;
    }
  }
}
