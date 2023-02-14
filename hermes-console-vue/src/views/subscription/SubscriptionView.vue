<script setup lang="ts">
  import { useRoute } from 'vue-router';
  import { useSubscription } from '@/composables/useSubscription';
  import ConsoleAlert from '@/components/console-alert/ConsoleAlert.vue';
  import FiltersCard from '@/views/subscription/filters-card/FiltersCard.vue';
  import HealthProblemsAlerts from '@/views/subscription/health-problems-alerts/HealthProblemsAlerts.vue';
  import LastUndeliveredMessage from '@/views/subscription/last-undelivered-message/LastUndeliveredMessage.vue';
  import LoadingSpinner from '@/components/loading-spinner/LoadingSpinner.vue';
  import ManageMessagesCard from '@/views/subscription/manage-messages-card/ManageMessagesCard.vue';
  import MetricsCard from '@/views/subscription/metrics-card/MetricsCard.vue';
  import PropertiesCard from '@/views/subscription/properties-card/PropertiesCard.vue';
  import ServiceResponseMetrics from '@/views/subscription/service-response-metrics/ServiceResponseMetrics.vue';
  import SubscriptionBreadcrumbs from '@/views/subscription/subscription-breadcrumbs/SubscriptionBreadcrumbs.vue';
  import SubscriptionHeader from '@/views/subscription/subscription-header/SubscriptionHeader.vue';
  import UndeliveredMessages from '@/views/subscription/undelivered-messages/UndeliveredMessages.vue';

  const route = useRoute();
  const params = route.params as Record<string, string>;
  const { groupId, subscriptionId, topicId } = params;

  const { subscription, metrics, health, error, loading } = useSubscription(
    topicId,
    subscriptionId,
  );

  const authorized = true;
</script>

<template>
  <v-container class="d-flex flex-column subscription-view">
    <subscription-breadcrumbs
      :group-id="groupId"
      :topic-id="topicId"
      :subscription-id="subscriptionId"
    />

    <loading-spinner v-if="loading" />
    <console-alert
      v-if="error"
      title="Connection error"
      :text="`An unknown error occurred when fetching ${subscriptionId} subscription details`"
      type="error"
    />

    <subscription-header
      v-if="!loading"
      :subscription="subscription"
      :authorized="authorized"
    />

    <health-problems-alerts v-if="health" :problems="health.problems" />

    <div v-if="!loading" class="d-flex flex-row subscription-view__row">
      <div class="d-flex flex-column flex-grow-1 subscription-view__column">
        <metrics-card :metrics="metrics" />
        <service-response-metrics />
        <manage-messages-card />
        <last-undelivered-message />
      </div>
      <div class="d-flex flex-column flex-grow-1 subscription-view__column">
        <properties-card :subscription="subscription" />
      </div>
    </div>

    <filters-card />
    <undelivered-messages />
  </v-container>
</template>

<style scoped lang="scss">
  .subscription-view {
    row-gap: 8px;

    &__row {
      column-gap: 8px;
    }

    &__column {
      row-gap: 8px;
    }
  }
</style>
