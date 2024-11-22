<script setup lang="ts">


import type {TrackingUrl} from "@/api/topic";

const props = defineProps<{
  trackingUrls: TrackingUrl[]
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
    <v-card-item v-if="trackingUrls">
      <p v-for="trackingUrl in trackingUrls">
        <v-btn
            :href="trackingUrl.url"
            target="_blank"
            variant="text"
            color="green"
        >
          {{ trackingUrl.name }}
        </v-btn>
      </p>
    </v-card-item>
    <v-card-item v-else>
      No tracking urls available
    </v-card-item>
  </v-card>
</template>

<style scoped lang="scss"></style>
