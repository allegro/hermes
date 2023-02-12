<script setup lang="ts">
import StatisticsItem from "@/views/topic/components/statistics-list/statistics-item/StatisticsItem.vue";
import { useTopicMetrics } from "@/composables/topic/useTopicMetrics";

interface Props {
  topic: string;
}

const { topic } = defineProps<Props>();
const { metrics, error } = useTopicMetrics(topic);
</script>

<template>
  <v-card v-if="metrics" class="d-flex flex-column">
    <v-card-title>Statistics</v-card-title>
    <v-card-text class="d-flex flex-column">
      <statistics-item
        name="Rate"
        :value="metrics.rate"
        icon="mdi-speedometer-slow"
      />
      <statistics-item
        name="Delivery rate"
        :value="metrics.deliveryRate"
        icon="mdi-speedometer-slow"
      />
      <statistics-item
        name="Published"
        :value="metrics.published"
        icon="mdi-message-text-outline"
      />
      <statistics-item
        name="Latency"
        value="?"
        icon="mdi-clock-fast"
      />
      <statistics-item
        name="Message size"
        value="?"
        icon="mdi-scale"
      />
    </v-card-text>
  </v-card>
  <v-alert
    v-else-if="error"
    text="Failed fetching metrics for topic"
    type="error"
    variant="tonal"
  />
</template>

<style scoped lang="scss">
</style>
