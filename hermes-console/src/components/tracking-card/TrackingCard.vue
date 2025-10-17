<script setup lang="ts">
  import SimpleLink from '@/components/link/SimpleLink.vue';
  import type { TrackingUrl } from '@/api/tracking-url';

  const props = defineProps<{
    trackingUrls: TrackingUrl[];
  }>();
</script>

<template>
  <v-card>
    <v-card-item class="border-b">
      <v-card-title class="font-weight-bold">
        {{ $t('trackingCard.title') }}
      </v-card-title>
    </v-card-item>
    <v-list
      v-if="props.trackingUrls && props.trackingUrls.length > 0"
      open-strategy="single"
    >
      <v-list-item
        v-for="trackingUrl in props.trackingUrls"
        :key="trackingUrl.name"
        :href="trackingUrl.url"
      >
        <simple-link
          :href="trackingUrl.url"
          :text="trackingUrl.name"
          open-in-new-tab
        />
      </v-list-item>
    </v-list>
    <v-card-item v-else> {{ $t('trackingCard.noTrackingUrls') }}</v-card-item>
  </v-card>
</template>

<style scoped lang="scss"></style>
