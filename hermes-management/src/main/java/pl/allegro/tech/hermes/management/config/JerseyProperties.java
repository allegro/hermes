package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "jersey")
public class JerseyProperties {

    private List<String> packagesToScan = new ArrayList<>();
    private String filterStaticContentRegexp = "/(status|components|css|img|js|partials|assets)/.*";

    public List<String> getPackagesToScan() {
        return packagesToScan;
    }

    public void setPackagesToScan(List<String> packagesToScan) {
        this.packagesToScan = packagesToScan;
    }

    public String getFilterStaticContentRegexp() {
        return filterStaticContentRegexp;
    }

    public void setFilterStaticContentRegexp(String filterStaticContentRegexp) {
        this.filterStaticContentRegexp = filterStaticContentRegexp;
    }
}
