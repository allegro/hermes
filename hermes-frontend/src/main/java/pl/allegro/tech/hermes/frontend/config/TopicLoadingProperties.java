package pl.allegro.tech.hermes.frontend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "frontend.startup.topic.loading")
public class TopicLoadingProperties {

    private MetadataLoadingProperties metadata = new MetadataLoadingProperties();

    private SchemaLoadingProperties schema = new SchemaLoadingProperties();

    private MetadataRefreshJobProperties metadataRefreshJob = new MetadataRefreshJobProperties();

    public static class MetadataLoadingProperties{

        private boolean enabled = false;

        private long retryInterval = 1_000L;

        private int retryCount = 5;

        private int threadPoolSize = 16;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getRetryInterval() {
            return retryInterval;
        }

        public void setRetryInterval(long retryInterval) {
            this.retryInterval = retryInterval;
        }

        public int getRetryCount() {
            return retryCount;
        }

        public void setRetryCount(int retryCount) {
            this.retryCount = retryCount;
        }

        public int getThreadPoolSize() {
            return threadPoolSize;
        }

        public void setThreadPoolSize(int threadPoolSize) {
            this.threadPoolSize = threadPoolSize;
        }
    }

    public static class SchemaLoadingProperties{

        private boolean enabled = false;

        private int retryCount = 3;

        private int threadPoolSize = 16;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getRetryCount() {
            return retryCount;
        }

        public void setRetryCount(int retryCount) {
            this.retryCount = retryCount;
        }

        public int getThreadPoolSize() {
            return threadPoolSize;
        }

        public void setThreadPoolSize(int threadPoolSize) {
            this.threadPoolSize = threadPoolSize;
        }
    }

    public static class MetadataRefreshJobProperties {

        private boolean enabled = false;

        private int intervalSeconds = 60;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getIntervalSeconds() {
            return intervalSeconds;
        }

        public void setIntervalSeconds(int intervalSeconds) {
            this.intervalSeconds = intervalSeconds;
        }
    }

    public MetadataLoadingProperties getMetadata() {
        return metadata;
    }

    public void setMetadata(MetadataLoadingProperties metadata) {
        this.metadata = metadata;
    }

    public SchemaLoadingProperties getSchema() {
        return schema;
    }

    public void setSchema(SchemaLoadingProperties schema) {
        this.schema = schema;
    }

    public MetadataRefreshJobProperties getMetadataRefreshJob() {
        return metadataRefreshJob;
    }

    public void setMetadataRefreshJob(MetadataRefreshJobProperties metadataRefreshJob) {
        this.metadataRefreshJob = metadataRefreshJob;
    }
}
