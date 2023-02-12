<script async setup lang="ts">
import StatisticsList from "@/views/topic/components/statistics-list/StatisticsList.vue";
import PropertiesList from "@/views/topic/components/properties-list/PropertiesList.vue";
import { useTopic } from "@/composables/useTopic";
import SchemaPanel from "@/views/topic/components/schema-panel/SchemaPanel.vue";
import MessagesPreview from "@/views/topic/components/messages-preview/MessagesPreview.vue";

const topicName = "pl.allegro.public.group.DummyEvent";
const { topic, owner, ownerError } = useTopic(topicName);
</script>

<template>
  <v-container v-if="topic" class="d-flex flex-column topic-view__container">
    <div class="d-flex justify-space-between align-center">
      <v-breadcrumbs
        :items="['home', 'groups', 'pl.allegro.public.group', 'DummyEvent']" />
      <v-tooltip text="Copy event name">
        <template v-slot:activator="{ props }">
          <v-btn icon="mdi-content-copy" variant="plain" v-bind="props" />
        </template>
      </v-tooltip>
    </div>

    <div>
      {{ ownerError }}
      <span v-if="owner">Owner: {{ owner }}</span>
      <v-alert v-else-if="ownerError" text="Failed getting topic owner"></v-alert>
    </div>

    <div>
      <span>{{ topic.description }}</span>
    </div>

    <div class="topic__upper_panel">
      <statistics-list :topic="topicName" />
      <properties-list :topic="topic" />
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
