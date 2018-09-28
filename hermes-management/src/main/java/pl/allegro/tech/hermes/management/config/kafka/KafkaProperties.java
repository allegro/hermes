package pl.allegro.tech.hermes.management.config.kafka;

public class KafkaProperties {

    private String clusterName = "primary";

    private String connectionString = "localhost:2181";

    private int sessionTimeout = 10000;

    private int connectionTimeout = 3000;

    private int retryTimes = 3;

    private int retrySleep = 1000;

    private String offsetsStorage = "kafka";

    private boolean dualCommitEnabled = false;

    private String namespace = "";

    private KafkaConsumer kafkaConsumer = new KafkaConsumer();

    public static final class KafkaConsumer {

        private int cacheExpiration = 60;

        private int bufferSize = 64 * 1024;

        private int timeout = 5000;

        private String namePrefix = "offsetChecker";

        private int pollTimeoutMillis = 30;

        private String consumerGroupName = "RETRANSMISSION_GROUP";

        public int getCacheExpiration() {
            return cacheExpiration;
        }

        public void setCacheExpiration(int cacheExpiration) {
            this.cacheExpiration = cacheExpiration;
        }

        public int getBufferSize() {
            return bufferSize;
        }

        public void setBufferSize(int bufferSize) {
            this.bufferSize = bufferSize;
        }

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }

        public String getNamePrefix() {
            return namePrefix;
        }

        public void setNamePrefix(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        public int getPollTimeoutMillis() {
            return pollTimeoutMillis;
        }

        public void setPollTimeoutMillis(int pollTimeoutMillis) {
            this.pollTimeoutMillis = pollTimeoutMillis;
        }

        public String getConsumerGroupName() {
            return consumerGroupName;
        }

        public void setConsumerGroupName(String consumerGroupName) {
            this.consumerGroupName = consumerGroupName;
        }
    }

    public String getConnectionString() {
        return connectionString;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    public int getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getRetryTimes() {
        return retryTimes;
    }

    public void setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
    }

    public int getRetrySleep() {
        return retrySleep;
    }

    public void setRetrySleep(int retrySleep) {
        this.retrySleep = retrySleep;
    }

    public KafkaConsumer getKafkaConsumer() {
        return kafkaConsumer;
    }

    public void setKafkaConsumer(KafkaConsumer kafkaConsumer) {
        this.kafkaConsumer = kafkaConsumer;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getOffsetsStorage() {
        return offsetsStorage;
    }

    public void setOffsetsStorage(String offsetsStorage) {
        this.offsetsStorage = offsetsStorage;
    }

    public boolean isDualCommitEnabled() {
        return dualCommitEnabled;
    }

    public void setDualCommitEnabled(boolean dualCommitEnabled) {
        this.dualCommitEnabled = dualCommitEnabled;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
}
