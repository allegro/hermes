<script setup lang="ts">
  import { useI18n } from 'vue-i18n';
  import { useRoute } from 'vue-router';
  import { useSubscription } from '@/composables/use-subscription/useSubscription';
  import ConsoleAlert from '@/components/console-alert/ConsoleAlert.vue';
  import FiltersCard from '@/views/subscription/filters-card/FiltersCard.vue';
  import HeadersCard from '@/views/subscription/headers-card/HeadersCard.vue';
  import HealthProblemsAlerts from '@/views/subscription/health-problems-alerts/HealthProblemsAlerts.vue';
  import LastUndeliveredMessage from '@/views/subscription/last-undelivered-message/LastUndeliveredMessage.vue';
  import LoadingSpinner from '@/components/loading-spinner/LoadingSpinner.vue';
  import ManageMessagesCard from '@/views/subscription/manage-messages-card/ManageMessagesCard.vue';
  import MetricsCard from '@/views/subscription/metrics-card/MetricsCard.vue';
  import PropertiesCard from '@/views/subscription/properties-card/PropertiesCard.vue';
  import ServiceResponseMetrics from '@/views/subscription/service-response-metrics/ServiceResponseMetrics.vue';
  import ShowEventTrace from '@/views/subscription/show-event-trace/ShowEventTrace.vue';
  import SubscriptionBreadcrumbs from '@/views/subscription/subscription-breadcrumbs/SubscriptionBreadcrumbs.vue';
  import SubscriptionMetadata from '@/views/subscription/subscription-metadata/SubscriptionMetadata.vue';
  import UndeliveredMessagesCard from '@/views/subscription/undelivered-messages-card/UndeliveredMessagesCard.vue';

  const route = useRoute();
  const params = route.params as Record<string, string>;
  const { groupId, subscriptionId, topicId } = params;

  const { t } = useI18n();

  const {
    subscription,
    subscriptionMetrics,
    subscriptionHealth,
    subscriptionUndeliveredMessages,
    subscriptionLastUndeliveredMessage,
    error,
    loading,
  } = useSubscription(topicId, subscriptionId);

  const authorized = true;
</script>

<template>
  <v-container>
    <v-row dense>
      <v-col md="12">
        <subscription-breadcrumbs
          :group-id="groupId"
          :topic-id="topicId"
          :subscription-id="subscriptionId"
        />
        <loading-spinner v-if="loading" />
        <console-alert
          v-if="error"
          :title="t('subscription.connectionError.title')"
          :text="t('subscription.connectionError.text', { subscriptionId })"
          type="error"
        />
      </v-col>
    </v-row>

    <template v-if="!loading && !error">
      <v-row dense>
        <v-col md="12">
          <health-problems-alerts
            v-if="subscriptionHealth && subscriptionHealth.problems"
            :problems="subscriptionHealth?.problems"
          />
          <subscription-metadata
            v-if="subscription"
            :subscription="subscription"
            :authorized="authorized"
          />
        </v-col>
      </v-row>

      <v-row dense>
        <v-col md="6">
          <metrics-card
            v-if="subscriptionMetrics"
            :subscription-metrics="subscriptionMetrics"
          />
          <service-response-metrics />
          <manage-messages-card />
        </v-col>
        <v-col md="6">
          <properties-card v-if="subscription" :subscription="subscription" />
        </v-col>
      </v-row>

      <v-row dense>
        <v-col md="6">
          <last-undelivered-message
            v-if="subscriptionLastUndeliveredMessage"
            :last-undelivered="subscriptionLastUndeliveredMessage"
          />
        </v-col>
        <v-col md="6">
          <show-event-trace /><!-- v-if="subscription?.trackingEnabled" -->
        </v-col>
      </v-row>

      <v-row dense>
        <v-col md="12">
          <filters-card
            v-if="subscription && subscription?.filters.length > 0"
            :filters="subscription?.filters"
          />
          <headers-card
            v-if="!!subscription && subscription.headers.length > 0"
            :headers="subscription?.headers"
          />
          <undelivered-messages-card
            v-if="
              subscriptionUndeliveredMessages &&
              subscriptionUndeliveredMessages?.length > 0
            "
            :undelivered-messages="subscriptionUndeliveredMessages"
          />
        </v-col>
      </v-row>
    </template>
  </v-container>
</template>

<style scoped lang="scss"></style>
