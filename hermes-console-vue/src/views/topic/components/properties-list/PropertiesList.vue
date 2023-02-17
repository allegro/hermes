<script setup lang="ts">
import type { TopicWithSchema } from "@/api/topic";
import { Ack } from "@/api/topic";
import KeyValueCard from "@/components/key-value-card/KeyValueCard.vue";
import KeyValueCardItem from "@/components/key-value-card/key-value-card-item/KeyValueCardItem.vue";
import { useI18n } from "vue-i18n";

interface Props {
  topic: TopicWithSchema;
}

const { topic } = defineProps<Props>();

const { t } = useI18n();

const ackTexts = new Map<Ack, String>([
  [Ack.ALL, "All replicas"],
  [Ack.LEADER, "Leader only"],
  [Ack.NONE, "None"]
]);
const offlineRetentionText = topic.offlineStorage.retentionTime.infinite ? "infinite" : `${ topic.offlineStorage.retentionTime.duration } days`;
const authorizedPublishers = topic.auth.publishers.length === 0 ? "Not set" : topic.auth.publishers.join(", ");
const labels = topic.labels.map((label) => label.value).join(", ");
const retentionTime = `${ topic.retentionTime.duration } ${ topic.retentionTime.retentionUnit.toLowerCase() }`;
const ackText = ackTexts.get(topic.ack);
</script>

<template>
  <key-value-card :title="t('topicView.properties.header')">
    <key-value-card-item :name="t('topicView.properties.contentType')" :value="topic.contentType" />
    <key-value-card-item :name="t('topicView.properties.labels')" :value="labels" />
    <key-value-card-item
      :name="t('topicView.properties.acknowledgement')"
      :value="ackText"
      :tooltip="t('topicView.properties.tooltips.acknowledgement')"
    />
    <key-value-card-item
      :name="t('topicView.properties.retentionTime')"
      :value="retentionTime"
      :tooltip="t('topicView.properties.tooltips.retentionTime')"
    />
    <key-value-card-item :name="t('topicView.properties.trackingEnabled')" :value="topic.trackingEnabled" />
    <key-value-card-item :name="t('topicView.properties.maxMessageSize')" :value="topic.maxMessageSize" />
    <key-value-card-item
      :name="t('topicView.properties.schemaIdAwareSerializationEnabled')"
      :value="topic.schemaIdAwareSerializationEnabled"
    />
    <key-value-card-item :name="t('topicView.properties.authorizationEnabled')" :value="topic.auth.enabled" />
    <key-value-card-item
      :name="t('topicView.properties.authorizedPublishers')"
      :value="authorizedPublishers"
      :tooltip="t('topicView.properties.tooltips.authorizedPublishers')"
    />
    <key-value-card-item
      :name="t('topicView.properties.allowUnauthenticatedAccess')"
      :value="topic.auth.unauthenticatedAccessEnabled"
      :tooltip="t('topicView.properties.tooltips.allowUnauthenticatedAccess')"
    />
    <key-value-card-item
      :name="t('topicView.properties.restrictSubscribing')"
      :value="topic.subscribingRestricted"
      :tooltip="t('topicView.properties.tooltips.restrictSubscribing')"
    />
    <key-value-card-item
      :name="t('topicView.properties.storeOffline')"
      :value="topic.offlineStorage.enabled"
      :tooltip="t('topicView.properties.tooltips.storeOffline')"
    />
    <key-value-card-item
      :name="t('topicView.properties.offlineRetention')"
      :value="offlineRetentionText"
      :tooltip="t('topicView.properties.tooltips.offlineRetention')"
    />
    <key-value-card-item :name="t('topicView.properties.creationDate')" :value="topic.createdAt" />
    <key-value-card-item :name="t('topicView.properties.modificationDate')" :value="topic.modifiedAt" />
  </key-value-card>
</template>

<style scoped lang="scss"></style>
