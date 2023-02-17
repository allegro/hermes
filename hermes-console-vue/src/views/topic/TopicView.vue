<script async setup lang="ts">
import StatisticsList from "@/views/topic/components/metrics-list/MetricsList.vue";
import PropertiesListV2 from "@/views/topic/components/properties-list/PropertiesList.vue";
import { useTopic } from "@/composables/topic/useTopic";
import SchemaPanel from "@/views/topic/components/schema-panel/SchemaPanel.vue";
import MessagesPreview from "@/views/topic/components/messages-preview/MessagesPreview.vue";
import TopicHeader from "@/views/topic/components/topic-header/TopicHeader.vue";

const topicName = "pl.allegro.public.group.DummyEvent";
const { topic, owner, ownerError } = useTopic(topicName);
</script>

<template>
  <v-container v-if="topic" class="d-flex flex-column topic-view__container">
    <div class="d-flex justify-space-between align-center">
      <v-breadcrumbs
        :items="['home', 'groups', 'pl.allegro.public.group', 'DummyEvent']" />
    </div>

    <topic-header v-if="topic" :topic="topic" :owner="owner"/>

    <div class="topic__upper_panel">
      <statistics-list :topic="topicName" />
      <properties-list-v2 :topic="topic" />
    </div>

    <schema-panel :schema="topic.schema" />

    <messages-preview />
  </v-container>
</template>

<style scoped lang="scss">
.topic-view__container {
  row-gap: 8pt;
}

.topic__upper_panel {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  grid-gap: 8pt;
  align-items: start;
}
</style>
