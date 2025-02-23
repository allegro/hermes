<script setup lang="ts">
  const props = defineProps<{
    iframeUrl: string;
    detailsUrl: string;
  }>();
</script>

<template>
  <v-card border flat>
    <template #title>
      <div class="d-flex justify-space-between">
        <p class="font-weight-bold">
          {{ $t('costsCard.title') }}
        </p>
        <v-btn
          class="text-none"
          prepend-icon="mdi-open-in-new"
          target="_blank"
          :href="detailsUrl"
          variant="text"
        >
          {{ $t('costsCard.detailsButton') }}
        </v-btn>
      </div>
    </template>
    <v-card-item>
      <iframe
        height="100%"
        style="border: none"
        width="100%"
        :src="props.iframeUrl"
      />
    </v-card-item>
  </v-card>
</template>

<style scoped lang="scss"></style>
