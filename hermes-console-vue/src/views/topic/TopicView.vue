<script async setup lang="ts">
  import { useI18n } from 'vue-i18n';
  import { useRoute } from 'vue-router';
  import { useSubscriptionsList } from '@/composables/topic-subscriptions/use-subscriptions-list/useSubscriptionsList';
  import { useTopic } from '@/composables/topic/use-topic/useTopic';
  import { useTopicMetrics } from '@/composables/topic/use-topic-metric/useTopicMetrics';
  import ConsoleAlert from '@/components/console-alert/ConsoleAlert.vue';
  import LoadingSpinner from '@/components/loading-spinner/LoadingSpinner.vue';
  import MessagesPreview from '@/views/topic/components/messages-preview/MessagesPreview.vue';
  import MetricsList from '@/views/topic/components/metrics-list/MetricsList.vue';
  import PropertiesList from '@/views/topic/components/properties-list/PropertiesList.vue';
  import SchemaPanel from '@/views/topic/components/schema-panel/SchemaPanel.vue';
  import SubscriptionsList from './components/subscriptions-list/SubscriptionsList.vue';
  import TopicHeader from '@/views/topic/components/topic-header/TopicHeader.vue';
  import { useTopicMessagesPreview } from '@/composables/topic/use-topic-messages-preview/useTopicMessagesPreview';

  const { t } = useI18n();
  const route = useRoute();
  const { groupId, topicName } = route.params as Record<string, string>;
  const { topic, owner, topicIsLoading, ownerIsLoading, topicError } =
    useTopic(topicName);
  const { subscriptions } = useSubscriptionsList(topicName);
  const { data: metrics } = useTopicMetrics(topicName);
  const { data: messages } = useTopicMessagesPreview(topicName);

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
  <v-container class="d-flex flex-column topic-view__container">
    <div class="d-flex justify-space-between align-center">
      <v-breadcrumbs :items="breadcrumbsItems" density="compact" />
    </div>
    <loading-spinner v-if="topicIsLoading || ownerIsLoading" />
    <console-alert
      v-if="topicError"
      :text="
        t('topicView.errorMessage.topicFetchFailed', { topicName: topicName })
      "
      type="error"
    />

    <topic-header v-if="topic && owner" :topic="topic" :owner="owner" />

    <div class="topic-view__upper_panel">
      <metrics-list v-if="metrics" :metrics="metrics" />
      <properties-list v-if="topic" :topic="topic" />
    </div>

    <schema-panel v-if="topic" :schema="topic.schema" />

    <messages-preview v-if="messages" :messages="messages" />

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
