package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URL;
import java.util.Optional;

@ConfigurationProperties(prefix = "supportTeam")
public class SupportTeamServiceProperties {

    private SupportTeamServiceType type;

    private CrowdProperties crowd;

    private Long cacheDurationSeconds = 300L;

    private Long cacheSize = 1000L;

    private boolean enabled = false;

    public Optional<SupportTeamServiceType> getType() {
        return Optional.ofNullable(type);
    }

    public void setType(SupportTeamServiceType type) {
        this.type = type;
    }

    public CrowdProperties getCrowd() {
        return crowd;
    }

    public void setCrowd(CrowdProperties crowd) {
        this.crowd = crowd;
    }

    public Long getCacheDurationSeconds() {
        return cacheDurationSeconds;
    }

    public void setCacheDurationSeconds(Long cacheDurationSeconds) {
        this.cacheDurationSeconds = cacheDurationSeconds;
    }

    public Long getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(Long cacheSize) {
        this.cacheSize = cacheSize;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public enum SupportTeamServiceType {
        CROWD
    }

    public static class CrowdProperties {

        private String userName;

        private String password;

        private URL path;

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public URL getPath() {
            return path;
        }

        public void setPath(URL path) {
            this.path = path;
        }
    }
}
