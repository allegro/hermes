<script async setup lang="ts">
  import { formatNumber } from '../../../utils/number-formatter/number-formatter';
  import { useI18n } from 'vue-i18n';
  import { useStats } from '@/composables/use-stats/useStats';

  const { t } = useI18n();

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
          :title="t('stats.connectionError.title')"
          :text="t('stats.connectionError.text')"
          type="error"
        />
      </v-col>
    </v-row>
    <v-row>
      <h1>{{ $t('stats.topics') }}</h1>
    </v-row>
    <v-row>
      <v-col md="3">
        <v-row>
          <h2>{{ $t('stats.total') }}</h2>
        </v-row>
        <v-row>
          <h2>{{ formatNumber(topicCount) }}</h2>
        </v-row>
      </v-col>
      <v-col md="3">
        <v-row>
          <h2>{{ $t('stats.ackAll') }}</h2>
        </v-row>
        <v-row>
          <h2>
            {{ formatNumber(ackAllTopicCount) }} ({{
              formatNumber(ackAllTopicShare, 2)
            }}%)
          </h2>
        </v-row>
      </v-col>
      <v-col md="3">
        <v-row>
          <h2>{{ $t('stats.trackingEnabled') }}</h2>
        </v-row>
        <v-row>
          <h2>
            {{ formatNumber(trackingEnabledTopicCount) }} ({{
              formatNumber(trackingEnabledTopicShare, 2)
            }}%)
          </h2>
        </v-row>
      </v-col>
      <v-col md="3">
        <v-row>
          <h2>Avro</h2>
        </v-row>
        <v-row>
          <h2>
            {{ formatNumber(avroTopicCount) }} ({{
              formatNumber(avroTopicShare, 2)
            }}%)
          </h2>
        </v-row>
      </v-col>
    </v-row>
    <v-row>
      <h1>{{ $t('stats.subscriptions') }}</h1>
    </v-row>
    <v-row>
      <v-col md="3">
        <v-row>
          <h2>{{ $t('stats.total') }}</h2>
        </v-row>
        <v-row>
          <h2>{{ formatNumber(subscriptionCount) }}</h2>
        </v-row>
      </v-col>
      <v-col md="3">
        <v-row>
          <h2>{{ $t('stats.trackingEnabled') }}</h2>
        </v-row>
        <v-row>
          <h2>
            {{ formatNumber(trackingEnabledSubscriptionCount) }} ({{
              formatNumber(trackingEnabledSubscriptionShare, 2)
            }}%)
          </h2>
        </v-row>
      </v-col>
      <v-col md="3">
        <v-row>
          <h2>Avro</h2>
        </v-row>
        <v-row>
          <h2>
            {{ formatNumber(avroSubscriptionCount) }} ({{
              formatNumber(avroSubscriptionShare, 2)
            }}%)
          </h2>
        </v-row>
      </v-col>
    </v-row>
  </v-container>
</template>
