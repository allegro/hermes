<script async setup lang="ts">
  import { useStats } from '@/composables/use-stats/useStats';

  const {
    topicCount,
    ackAllTopicCount,
    ackAllTopicShare,
    trackingEnabledTopicCount,
    trackingEnabledTopicShare,
    avroTopicCount,
    avroTopicShare,
    subscriptionCount,
    trackingEnabledSubscriptionCount,
    trackingEnabledSubscriptionShare,
    avroSubscriptionCount,
    avroSubscriptionShare,
    loading,
    error,
  } = useStats();
</script>

<template>
  <v-container>
    <v-row class="home__logo" justify="center" cols="12">
      <v-col md="12">
        <loading-spinner v-if="loading" />
        <console-alert
          v-if="error"
          :title="$t('constraints.connectionError.title')"
          :text="$t('constraints.connectionError.text')"
          type="error"
        />
      </v-col>
    </v-row>
    <v-row>
      {{ topicCount }}
      {{ ackAllTopicCount }}
      {{ ackAllTopicShare }}
      {{ trackingEnabledTopicCount }}
      {{ trackingEnabledTopicShare }}
      {{ avroTopicCount }}
      {{ avroTopicShare }}
      {{ subscriptionCount }}
      {{ trackingEnabledSubscriptionCount }}
      {{ trackingEnabledSubscriptionShare }}
      {{ avroSubscriptionCount }}
      {{ avroSubscriptionShare }}
    </v-row>
  </v-container>
</template>
