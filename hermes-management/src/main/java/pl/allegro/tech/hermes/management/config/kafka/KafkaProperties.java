package pl.allegro.tech.hermes.management.config.kafka;

public class KafkaProperties {

    private String datacenter = "datacenter";

    private String clusterName = "primary";

    private String bootstrapKafkaServer = "localhost:9093";

    private int kafkaServerRequestTimeoutMillis = 3000;

    private int sessionTimeoutMillis = 10000;

    private int connectionTimeoutMillis = 3000;

    private int maxInflight = 10;

    private int retryTimes = 3;

    private int retrySleepMillis = 1000;

    private String offsetsStorage = "kafka";

    private boolean dualCommitEnabled = false;

    private String namespace = "";

    private KafkaConsumer kafkaConsumer = new KafkaConsumer();

    private KafkaSaslProperties sasl = new KafkaSaslProperties();

    public static final class KafkaConsumer {

        private int cacheExpirationSeconds = 60;

        private int bufferSizeBytes = 64 * 1024;

        private int timeoutMillis = 5000;

        private String namePrefix = "offsetChecker";

        private int pollTimeoutMillis = 50;

        private final int fetchMaxWaitMillis = 30;

        private final int fetchMinBytes = 1;

        private String consumerGroupName = "RETRANSMISSION_GROUP";

        public int getCacheExpirationSeconds() {
            return cacheExpirationSeconds;
        }

        public void setCacheExpirationSeconds(int cacheExpirationSeconds) {
            this.cacheExpirationSeconds = cacheExpirationSeconds;
        }

        public int getBufferSizeBytes() {
            return bufferSizeBytes;
        }

        public void setBufferSizeBytes(int bufferSizeBytes) {
            this.bufferSizeBytes = bufferSizeBytes;
        }

        public int getTimeoutMillis() {
            return timeoutMillis;
        }

        public void setTimeoutMillis(int timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
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

        public int getFetchMaxWaitMillis() {
            return fetchMaxWaitMillis;
        }

        public int getFetchMinBytes() {
            return fetchMinBytes;
        }
    }

    public int getSessionTimeoutMillis() {
        return sessionTimeoutMillis;
    }

    public void setSessionTimeoutMillis(int sessionTimeoutMillis) {
        this.sessionTimeoutMillis = sessionTimeoutMillis;
    }

    public int getConnectionTimeoutMillis() {
        return connectionTimeoutMillis;
    }

    public void setConnectionTimeoutMillis(int connectionTimeoutMillis) {
        this.connectionTimeoutMillis = connectionTimeoutMillis;
    }

    public int getRetryTimes() {
        return retryTimes;
    }

    public void setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
    }

    public int getRetrySleepMillis() {
        return retrySleepMillis;
    }

    public void setRetrySleepMillis(int retrySleepMillis) {
        this.retrySleepMillis = retrySleepMillis;
    }

    public KafkaConsumer getKafkaConsumer() {
        return kafkaConsumer;
    }

    public void setKafkaConsumer(KafkaConsumer kafkaConsumer) {
        this.kafkaConsumer = kafkaConsumer;
    }

    public KafkaSaslProperties getSasl() {
        return sasl;
    }

    public void setSasl(KafkaSaslProperties sasl) {
        this.sasl = sasl;
    }

    public String getDatacenter() {
        return datacenter;
    }

    public void setDatacenter(String datacenter) {
        this.datacenter = datacenter;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getQualifiedClusterName() {
        return clusterName + "-" + datacenter;
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

    public int getMaxInflight() {
        return maxInflight;
    }

    public void setMaxInflight(int maxInflight) {
        this.maxInflight = maxInflight;
    }

    public String getBootstrapKafkaServer() {
        return bootstrapKafkaServer;
    }

    public void setBootstrapKafkaServer(String bootstrapKafkaServer) {
        this.bootstrapKafkaServer = bootstrapKafkaServer;
    }

    public int getKafkaServerRequestTimeoutMillis() {
        return kafkaServerRequestTimeoutMillis;
    }

    public void setKafkaServerRequestTimeoutMillis(int kafkaServerRequestTimeoutMillis) {
        this.kafkaServerRequestTimeoutMillis = kafkaServerRequestTimeoutMillis;
    }
}
