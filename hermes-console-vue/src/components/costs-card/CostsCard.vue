<script setup lang="ts">
  const props = defineProps<{
    iframeUrl: string;
    detailsUrl: string;
  }>();
</script>

<template>
  <v-card>
    <template #title>
      <div class="d-flex justify-space-between">
        <p class="font-weight-bold">
          {{ $t('costsCard.title') }}
        </p>
        <v-card-actions>
          <v-btn
            class="text-none"
            prepend-icon="mdi-open-in-new"
            target="_blank"
            :href="detailsUrl"
          >
            {{ $t('costsCard.detailsButton') }}
          </v-btn>
        </v-card-actions>
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
