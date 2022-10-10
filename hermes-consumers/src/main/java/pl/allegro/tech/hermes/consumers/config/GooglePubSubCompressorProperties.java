package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "consumer.google.pubsub.compressor")
public class GooglePubSubCompressorProperties {

    private Boolean enabled = false;

    private String codecName = "bzip2";

    private String compressionLevel = "medium";

    private Long compressionThresholdBytes = 400L;

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

    public String getCompressionLevel() {
        return compressionLevel;
    }

    public void setCompressionLevel(String compressionLevel) {
        this.compressionLevel = compressionLevel;
    }

    public Long getCompressionThresholdBytes() {
        return compressionThresholdBytes;
    }

    public void setCompressionThresholdBytes(Long compressionThresholdBytes) {
        this.compressionThresholdBytes = compressionThresholdBytes;
    }
}
