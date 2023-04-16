<script async setup lang="ts">
  import { useI18n } from 'vue-i18n';
  import { useRoute } from 'vue-router';
  import { useSubscriptionsList } from '@/composables/topic-subcriptions/use-subscriptions-list/useSubscriptionsList';
  import { useTopic } from '@/composables/topic/use-topic/useTopic';
  import HeaderBreadcrumbs from '@/components/header-breadcrumbs/HeaderBreadcrumbs.vue';
  import MessagesPreview from '@/views/topic/components/messages-preview/MessagesPreview.vue';
  import PropertiesList from '@/views/topic/components/properties-list/PropertiesList.vue';
  import SchemaPanel from '@/views/topic/components/schema-panel/SchemaPanel.vue';
  import StatisticsList from '@/views/topic/components/metrics-list/MetricsList.vue';
  import SubscriptionsList from './components/subscriptions-list/SubscriptionsList.vue';
  import TopicHeader from '@/views/topic/components/topic-header/TopicHeader.vue';

  const { t } = useI18n();
  const route = useRoute();
  const { groupId, topicName } = route.params as Record<string, string>;
  const { topic, owner } = useTopic(topicName);
  const { subscriptions } = useSubscriptionsList(topicName);

  const breadcrumbsItems = [
    {
      title: t('subscription.subscriptionBreadcrumbs.home'),
      href: '/',
    },
    {
      title: t('subscription.subscriptionBreadcrumbs.groups'),
      href: '/groups',
    },
    {
      title: groupId,
      href: `/groups/${groupId}`,
    },
    {
      title: topicName,
      href: `/groups/${groupId}/topics/${topicName}`,
    },
  ];
</script>

<template>
  <v-container v-if="topic" class="d-flex flex-column topic-view__container">
    <div class="d-flex justify-space-between align-center">
      <header-breadcrumbs :items="breadcrumbsItems" />
    </div>

    <topic-header v-if="owner" :topic="topic" :owner="owner" />

    <div class="topic-view__upper_panel">
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

  .topic-view__upper_panel {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    grid-gap: 8pt;
    align-items: start;
  }
</style>
