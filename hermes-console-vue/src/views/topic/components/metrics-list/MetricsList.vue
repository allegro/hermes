<script setup lang="ts">
import { useTopicMetrics } from "@/composables/topic/useTopicMetrics";
import KeyValueCard from "@/components/key-value-card/KeyValueCard.vue";
import { useI18n } from "vue-i18n";
import KeyValueCardItem from "@/components/key-value-card/key-value-card-item/KeyValueCardItem.vue";

interface Props {
  topic: string;
}

const { topic } = defineProps<Props>();

const { t } = useI18n();
const { metrics, error } = useTopicMetrics(topic);
</script>

<template>
  <key-value-card v-if="metrics" :title="t('topicView.metrics.header')">
    <key-value-card-item :name="t('topicView.metrics.rate')" :value="metrics.rate" />
    <key-value-card-item :name="t('topicView.metrics.deliveryRate')" :value="metrics.deliveryRate" />
    <key-value-card-item :name="t('topicView.metrics.published')" :value="metrics.published" />
    <key-value-card-item :name="t('topicView.metrics.latency')" value="?" />
    <key-value-card-item :name="t('topicView.metrics.messageSize')" value="?" />
  </key-value-card>
</template>

<style scoped lang="scss">
</style>
