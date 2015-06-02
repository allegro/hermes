package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "jersey")
public class JerseyProperties {
    
    private List<String> packagesToScan = new ArrayList<>();

    public List<String> getPackagesToScan() {
        return packagesToScan;
    }
}
