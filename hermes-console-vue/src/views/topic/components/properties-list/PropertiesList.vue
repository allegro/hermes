<script setup lang="ts">
import type { TopicWithSchema } from "@/api/topic";
import { Ack } from "@/api/topic";
import PropertiesItem from "@/views/topic/components/properties-list/properties-item/PropertiesItem.vue";

interface Props {
  topic: TopicWithSchema;
}

const { topic } = defineProps<Props>();

const ackTexts = new Map<Ack, String>([
  [Ack.ALL, "All replicas"],
  [Ack.LEADER, "Leader only"],
  [Ack.NONE, "None"]
]);
const offlineRetentionText = topic.offlineStorage.retentionTime.infinite ? 'infinite' : `${topic.offlineStorage.retentionTime.duration} days`;
const authorizedPublishers = topic.auth.publishers.length === 0 ? 'Not set' : topic.auth.publishers.join(', ');
const labels = topic.labels.map((label) => label.value).join(", ");
const retentionTime = `${topic.retentionTime.duration} ${topic.retentionTime.retentionUnit.toLowerCase()}`;
const ackText = ackTexts.get(topic.ack);
</script>

<template>
  <v-card>
    <v-card-title>Properties</v-card-title>
    <v-card-text class="properties__list">
      <div class="d-flex flex-column properties__section">
        <span class="text-subtitle-1 text-disabled">Base properties</span>
        <properties-item name="Content type" :value="topic.contentType" />
        <properties-item name="Labels" :value="labels" />
        <properties-item name="Acknowlegment" :value="ackText" />
        <properties-item name="Retention time" :value="retentionTime" />
        <properties-item name="Tracking enabled" :value="topic.trackingEnabled " />
        <properties-item name="Max message size" :value="topic.maxMessageSize" />
        <properties-item name="SchemaId serialization enabled" :value="topic.schemaIdAwareSerializationEnabled" />
      </div>
      <div class="d-flex flex-column properties__section">
        <span class="text-subtitle-1 text-disabled">Authorization</span>
        <properties-item name="Authorisation enabled" :value="topic.auth.enabled" />
        <properties-item name="Authorised publishers" :value="authorizedPublishers" />
        <properties-item name="Allow unauthenticated access" :value="topic.auth.unauthenticatedAccessEnabled" />
      </div>
      <div class="d-flex flex-column properties__section">
        <span class="text-subtitle-1 text-disabled">Subscriptions</span>
        <properties-item name="Restrict subscribing" :value="topic.subscribingRestricted"/>
      </div>
      <div class="d-flex flex-column properties__section">
        <span class="text-subtitle-1 text-disabled">Storing</span>
        <properties-item name="Store offline" :value="topic.offlineStorage.enabled"/>
        <properties-item name="Offline retention" :value="offlineRetentionText"/>
      </div>
      <div class="d-flex flex-column properties__section">
        <span class="text-subtitle-1 text-disabled">Info</span>
        <properties-item name="Creation date" :value="topic.createdAt"/>
        <properties-item name="Modification date" :value="topic.modifiedAt"/>
      </div>
    </v-card-text>
  </v-card>
</template>

<style scoped lang="scss">
.properties__list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  grid-gap: 2pt;
  column-gap: 2pt;
}

.properties__section {
}
</style>
