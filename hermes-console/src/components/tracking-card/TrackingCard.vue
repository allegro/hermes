<script setup lang="ts">
  import type { TrackingUrl } from '@/api/tracking-url';

  const props = defineProps<{
    trackingUrls: TrackingUrl[];
  }>();
</script>

<template>
  <v-card>
    <template #title>
      <div class="d-flex justify-space-between">
        <p class="font-weight-bold">
          {{ $t('trackingCard.title') }}
        </p>
      </div>
    </template>
    <v-card-item v-if="props.trackingUrls && props.trackingUrls.length > 0">
      <p v-for="trackingUrl in props.trackingUrls" :key="trackingUrl.name">
        <v-btn
          :href="trackingUrl.url"
          target="_blank"
          variant="text"
          color="blue"
        >
          {{ trackingUrl.name }}
        </v-btn>
      </p>
    </v-card-item>
    <v-card-item v-else> {{ $t('trackingCard.noTrackingUrls') }} </v-card-item>
  </v-card>
</template>

<style scoped lang="scss"></style>
