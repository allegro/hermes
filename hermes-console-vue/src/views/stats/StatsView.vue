<script async setup lang="ts">
  import { formatNumber } from '@/utils/number-formatter/number-formatter';
  import { useI18n } from 'vue-i18n';
  import { useStats } from '@/composables/use-stats/useStats';
  import ConsoleAlert from '@/components/console-alert/ConsoleAlert.vue';
  import LoadingSpinner from '@/components/loading-spinner/LoadingSpinner.vue';

  const { t } = useI18n();

  const { stats, loading, error } = useStats();
</script>

<template>
  <v-container>
    <v-row v-if="loading || error" justify="center" cols="12">
      <v-col md="12">
        <loading-spinner v-if="loading" />
        <console-alert
          v-if="error.fetchError"
          :title="t('stats.connectionError.title')"
          :text="t('stats.connectionError.text')"
          type="error"
        />
      </v-col>
    </v-row>
    <div v-if="stats">
      <v-row>
        <h2 class="text-h2 mt-16">{{ $t('stats.title') }}</h2>
      </v-row>
      <v-row class="mt-16">
        <h3 class="text-h3">{{ $t('stats.topics') }}</h3>
      </v-row>
      <v-row class="mt-6">
        <v-col md="3">
          <v-row>
            <h5 class="text-h5">{{ $t('stats.total') }}</h5>
          </v-row>
          <v-row>
            <h4 class="text-h4">{{ formatNumber(stats.topicCount) }}</h4>
          </v-row>
        </v-col>
        <v-col md="3">
          <v-row>
            <h5 class="text-h5">{{ $t('stats.ackAll') }}</h5>
          </v-row>
          <v-row>
            <h4 class="text-h4">
              {{ formatNumber(stats.ackAllTopicCount) }} ({{
                formatNumber(stats.ackAllTopicShare, 2)
              }}%)
            </h4>
          </v-row>
        </v-col>
        <v-col md="3">
          <v-row>
            <h5 class="text-h5">{{ $t('stats.trackingEnabled') }}</h5>
          </v-row>
          <v-row>
            <h4 class="text-h4">
              {{ formatNumber(stats.trackingEnabledTopicCount) }} ({{
                formatNumber(stats.trackingEnabledTopicShare, 2)
              }}%)
            </h4>
          </v-row>
        </v-col>
        <v-col md="3">
          <v-row>
            <h5 class="text-h5">Avro</h5>
          </v-row>
          <v-row>
            <h4 class="text-h4">
              {{ formatNumber(stats.avroTopicCount) }} ({{
                formatNumber(stats.avroTopicShare, 2)
              }}%)
            </h4>
          </v-row>
        </v-col>
      </v-row>
      <v-row class="mt-16">
        <h3 class="text-h3">{{ $t('stats.subscriptions') }}</h3>
      </v-row>
      <v-row class="mt-6">
        <v-col md="3">
          <v-row>
            <h5 class="text-h5">{{ $t('stats.total') }}</h5>
          </v-row>
          <v-row>
            <h4 class="text-h4">
              {{ formatNumber(stats.subscriptionCount) }}
            </h4>
          </v-row>
        </v-col>
        <v-col md="3">
          <v-row>
            <h5 class="text-h5">{{ $t('stats.trackingEnabled') }}</h5>
          </v-row>
          <v-row>
            <h4 class="text-h4">
              {{ formatNumber(stats.trackingEnabledSubscriptionCount) }} ({{
                formatNumber(stats.trackingEnabledSubscriptionShare, 2)
              }}%)
            </h4>
          </v-row>
        </v-col>
        <v-col md="3">
          <v-row>
            <h5 class="text-h5">Avro</h5>
          </v-row>
          <v-row>
            <h4 class="text-h4">
              {{ formatNumber(stats.avroSubscriptionCount) }} ({{
                formatNumber(stats.avroSubscriptionShare, 2)
              }}%)
            </h4>
          </v-row>
        </v-col>
      </v-row>
    </div>
  </v-container>
</template>
