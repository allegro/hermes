<script async setup lang="ts">
  import { useTopic } from '@/composables/topic/useTopic';
  import MessagesPreview from '@/views/topic/components/messages-preview/MessagesPreview.vue';
  import PropertiesList from '@/views/topic/components/properties-list/PropertiesList.vue';
  import SchemaPanel from '@/views/topic/components/schema-panel/SchemaPanel.vue';
  import StatisticsList from '@/views/topic/components/metrics-list/MetricsList.vue';
  import SubscriptionsList from './components/subscriptions-list/SubscriptionsList.vue';
  import TopicHeader from '@/views/topic/components/topic-header/TopicHeader.vue';

  const topicName = 'pl.allegro.public.group.DummyEvent';
  const { topic, owner, subscriptions } = useTopic(topicName);
</script>

<template>
  <v-container v-if="topic" class="d-flex flex-column topic-view__container">
    <div class="d-flex justify-space-between align-center">
      <v-breadcrumbs
        :items="['home', 'groups', 'pl.allegro.public.group', topicName]"
      />
    </div>

    <topic-header v-if="topic" :topic="topic" :owner="owner" />

    <div class="topic__upper_panel">
      <statistics-list :topic="topicName" />
      <properties-list :topic="topic" />
    </div>

    <schema-panel :schema="topic.schema" />

    <messages-preview :topic-name="topicName" />

    <subscriptions-list v-if="subscriptions" :subscriptions="subscriptions" />
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
