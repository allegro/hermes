<script setup lang="ts">
  import { useRoute } from 'vue-router';
  import { useSubscription } from '@/composables/useSubscription';
  import ConsoleAlert from '@/components/console-alert/ConsoleAlert.vue';
  import LoadingSpinner from '@/components/loading-spinner/LoadingSpinner.vue';
  import ServiceResponseMetrics from '@/views/subscription/service-response-metrics/ServiceResponseMetrics.vue';
  import SubscriptionBreadcrumbs from '@/views/subscription/subscription-breadcrumbs/SubscriptionBreadcrumbs.vue';
  import SubscriptionHeader from '@/views/subscription/subscription-header/SubscriptionHeader.vue';
  import SubscriptionMessagesManager from '@/views/subscription/subscription-messages-manager/SubscriptionMessagesManager.vue';
  import SubscriptionMetrics from '@/views/subscription/subscription-metrics/SubscriptionMetrics.vue';
  import SubscriptionProperties from '@/views/subscription/subscription-properties/SubscriptionProperties.vue';

  const route = useRoute();
  const params = route.params as Record<string, string>;
  const { groupId, subscriptionId, topicId } = params;

  const { subscription, error, loading } = useSubscription(
    topicId,
    subscriptionId,
  );

  const authorized = false;
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
      v-if="subscription"
      :subscription="subscription"
      :authorized="authorized"
    />

    <console-alert
      title="Subscription health problems"
      text="Subscription lag is growing! Examine output rate and service response codes, looks like it is not consuming at full speed."
      type="warning"
      icon="mdi-speedometer-slow"
    />

    <div v-if="subscription" class="d-flex flex-row subscription-view__row">
      <div class="d-flex flex-column flex-grow-1 subscription-view__column">
        <subscription-metrics />
        <service-response-metrics />
        <subscription-messages-manager />
      </div>
      <div class="d-flex flex-column flex-grow-1 subscription-view__column">
        <subscription-properties :subscription="subscription" />
      </div>
    </div>
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
