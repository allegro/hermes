package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.TopicLabel;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@ConfigurationProperties(prefix = "topic")
public class TopicProperties {

    private int replicationFactor = 1;

    private int partitions = 10;

    private boolean allowRemoval = false;

    private boolean removeSchema = false;

    private List<ContentType> allowedContentTypes = Arrays.asList(ContentType.AVRO, ContentType.JSON);

    private Set<TopicLabel> allowedTopicLabels = Collections.emptySet();

    private boolean uncleanLeaderElectionEnabled = false;

    private int touchDelayInSeconds = 120;

    private boolean touchSchedulerEnabled = true;

    private int subscriptionsAssignmentsCompletedTimeoutSeconds = 30;

    private boolean defaultSchemaIdAwareSerializationEnabled = false;

    private boolean avroContentTypeMetadataRequired = true;

    /**
     * Introduced in Kafka 0.11.0.0 mechanism of splitting oversized batches does not respect configuration of maximum
     * message size which broker can accept. It can cause an infinite loop of resending the same records in one batch.
     * To avoid the issue this parameter should be greater than or equal to maximum size of request that the producer
     * can send (parameter kafka.producer.max.request.size). Note that if you change this setting, it is necessary to
     * manually update max.message.bytes for existing topics on broker side.
     *
     * For more information see:
     * https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=68715855
     * https://issues.apache.org/jira/browse/KAFKA-8202
     */
    private int maxMessageSize = 1024 * 1024;

    public int getReplicationFactor() {
        return replicationFactor;
    }

    public void setReplicationFactor(int replicationFactor) {
        this.replicationFactor = replicationFactor;
    }

    public int getPartitions() {
        return partitions;
    }

    public void setPartitions(int partitions) {
        this.partitions = partitions;
    }

    public boolean isAllowRemoval() {
        return allowRemoval;
    }

    public void setAllowRemoval(boolean allowRemoval) {
        this.allowRemoval = allowRemoval;
    }

    public boolean isRemoveSchema() {
        return removeSchema;
    }

    public void setRemoveSchema(boolean removeSchema) {
        this.removeSchema = removeSchema;
    }

    public List<ContentType> getAllowedContentTypes() {
        return allowedContentTypes;
    }

    public void setAllowedContentTypes(List<ContentType> allowedContentTypes) {
        this.allowedContentTypes = allowedContentTypes;
    }

    public Set<TopicLabel> getAllowedTopicLabels() {
        return allowedTopicLabels;
    }

    public void setAllowedTopicLabels(Set<TopicLabel> allowedTopicLabels) {
        this.allowedTopicLabels = allowedTopicLabels;
    }

    public boolean isUncleanLeaderElectionEnabled() {
        return uncleanLeaderElectionEnabled;
    }

    public void setUncleanLeaderElectionEnabled(boolean uncleanLeaderElectionEnabled) {
        this.uncleanLeaderElectionEnabled = uncleanLeaderElectionEnabled;
    }

    public int getTouchDelayInSeconds() {
        return touchDelayInSeconds;
    }

    public void setTouchDelayInSeconds(int touchDelayInSeconds) {
        this.touchDelayInSeconds = touchDelayInSeconds;
    }

    public boolean isTouchSchedulerEnabled() {
        return touchSchedulerEnabled;
    }

    public void setTouchSchedulerEnabled(boolean touchSchedulerEnabled) {
        this.touchSchedulerEnabled = touchSchedulerEnabled;
    }

    public int getSubscriptionsAssignmentsCompletedTimeoutSeconds() {
        return subscriptionsAssignmentsCompletedTimeoutSeconds;
    }

    public void setSubscriptionsAssignmentsCompletedTimeoutSeconds(int subscriptionsAssignmentsCompletedTimeoutSeconds) {
        this.subscriptionsAssignmentsCompletedTimeoutSeconds = subscriptionsAssignmentsCompletedTimeoutSeconds;
    }

    public int getMaxMessageSize() {
        return maxMessageSize;
    }

    public void setMaxMessageSize(int maxMessageSize) {
        this.maxMessageSize = maxMessageSize;
    }

    public void setDefaultSchemaIdAwareSerializationEnabled(boolean defaultSchemaIdAwareSerializationEnabled) {
        this.defaultSchemaIdAwareSerializationEnabled = defaultSchemaIdAwareSerializationEnabled;
    }

    public boolean isDefaultSchemaIdAwareSerializationEnabled() {
        return defaultSchemaIdAwareSerializationEnabled;
    }

    public boolean isAvroContentTypeMetadataRequired() {
        return avroContentTypeMetadataRequired;
    }

    public void setAvroContentTypeMetadataRequired(boolean avroContentTypeMetadataRequired) {
        this.avroContentTypeMetadataRequired = avroContentTypeMetadataRequired;
    }

}
