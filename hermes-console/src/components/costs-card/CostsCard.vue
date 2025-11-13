<script setup lang="ts">
  const props = defineProps<{
    iframeUrl: string;
    detailsUrl: string;
  }>();
</script>

<template>
  <v-card rounded="lg">
    <v-card-item class="border-b">
      <div class="d-flex justify-space-between align-start">
        <v-card-title class="font-weight-bold">
          {{ $t('costsCard.title') }}
        </v-card-title>

        <v-btn
          class="text-none"
          prepend-icon="mdi-open-in-new"
          target="_blank"
          :href="detailsUrl"
          variant="text"
          color="primary"
        >
          {{ $t('costsCard.detailsButton') }}
        </v-btn>
      </div>
    </v-card-item>

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
