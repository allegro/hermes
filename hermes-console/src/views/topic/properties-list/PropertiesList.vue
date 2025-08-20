<script setup lang="ts">
  import { formatTimestamp } from '@/utils/date-formatter/date-formatter';
  import { useI18n } from 'vue-i18n';
  import KeyValueCard from '@/components/key-value-card/KeyValueCard.vue';
  import KeyValueCardItem from '@/components/key-value-card/key-value-card-item/KeyValueCardItem.vue';
  import type { TopicWithSchema } from '@/api/topic';

  const props = defineProps<{
    topic: TopicWithSchema;
  }>();

  const { t } = useI18n();

  const offlineRetentionText = props.topic.offlineStorage.retentionTime.infinite
    ? 'infinite'
    : `${props.topic.offlineStorage.retentionTime.duration} days`;
  const authorizedPublishers =
    props.topic.auth.publishers?.length === 0
      ? t('topicView.properties.authorizedPublishersNotSet')
      : props.topic.auth.publishers?.join(', ');
  const labels = props.topic.labels.map((label) => label.value).join(', ');
  const retentionTime = `${
    props.topic.retentionTime.duration
  } ${props.topic.retentionTime.retentionUnit.toLowerCase()}`;
  const ackText = t(
    `topicView.properties.ackText.${props.topic.ack.toString().toLowerCase()}`,
  );
</script>

<template>
  <key-value-card :title="t('topicView.properties.title')">
    <key-value-card-item
      :name="$t('topicView.properties.contentType')"
      :value="topic.contentType"
    />
    <key-value-card-item
      :name="$t('topicView.properties.labels')"
      :value="labels"
    />
    <key-value-card-item
      :name="$t('topicView.properties.acknowledgement')"
      :value="ackText"
      :tooltip="t('topicView.properties.tooltips.acknowledgement')"
    />
    <key-value-card-item
      :name="$t('topicView.properties.retentionTime')"
      :value="retentionTime"
      :tooltip="t('topicView.properties.tooltips.retentionTime')"
    />
    <key-value-card-item
      :name="$t('topicView.properties.trackingEnabled')"
      :value="topic.trackingEnabled"
    />
    <key-value-card-item
      :name="$t('topicView.properties.maxMessageSize')"
      :value="topic.maxMessageSize"
    />
    <key-value-card-item
      :name="$t('topicView.properties.schemaIdAwareSerializationEnabled')"
      :value="topic.schemaIdAwareSerializationEnabled"
    />
    <key-value-card-item
      :name="$t('topicView.properties.authorizationEnabled')"
      :value="topic.auth.enabled"
    />
    <key-value-card-item
      :name="$t('topicView.properties.authorizedPublishers')"
      :value="authorizedPublishers"
      :tooltip="$t('topicView.properties.tooltips.authorizedPublishers')"
    />
    <key-value-card-item
      :name="$t('topicView.properties.allowUnauthenticatedAccess')"
      :value="topic.auth.unauthenticatedAccessEnabled"
      :tooltip="$t('topicView.properties.tooltips.allowUnauthenticatedAccess')"
    />
    <key-value-card-item
      :name="$t('topicView.properties.restrictSubscribing')"
      :value="topic.subscribingRestricted"
      :tooltip="$t('topicView.properties.tooltips.restrictSubscribing')"
    />
    <key-value-card-item
      :name="$t('topicView.properties.storeOffline')"
      :value="topic.offlineStorage.enabled"
      :tooltip="$t('topicView.properties.tooltips.storeOffline')"
    />
    <key-value-card-item
      :name="$t('topicView.properties.offlineRetention')"
      :value="offlineRetentionText"
      :tooltip="$t('topicView.properties.tooltips.offlineRetention')"
    />
    <key-value-card-item
      :name="$t('topicView.properties.creationDate')"
      :value="formatTimestamp(props.topic.createdAt)"
    />
    <key-value-card-item
      :name="$t('topicView.properties.modificationDate')"
      :value="formatTimestamp(props.topic.modifiedAt)"
    />
    <key-value-card-item
      :name="$t('topicView.properties.fallbackToRemoteDatacenterEnabled')"
      :value="topic.fallbackToRemoteDatacenterEnabled"
      :tooltip="
        $t('topicView.properties.tooltips.fallbackToRemoteDatacenterEnabled')
      "
    />
  </key-value-card>
</template>

<style scoped lang="scss"></style>
