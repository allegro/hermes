package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "consumer.google.pubsub.compressor")
public class GooglePubSubCompressorProperties {

    private Boolean enabled = false;

    private String codecName = "deflate";

    private int compressionLevel = 3;

    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getCodecName() {
        return codecName;
    }

    public void setCodecName(String codecName) {
        this.codecName = codecName;
    }

    public int getCompressionLevel() {
        return compressionLevel;
    }

    public void setCompressionLevel(int compressionLevel) {
        this.compressionLevel = compressionLevel;
    }
}
