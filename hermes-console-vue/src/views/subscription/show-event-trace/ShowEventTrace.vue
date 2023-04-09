<script setup lang="ts">
  import { useI18n } from 'vue-i18n';

  const { t } = useI18n();
</script>

<template>
  <v-card>
    <template #title>
      <p class="font-weight-bold">
        {{ t('subscription.showEventTrace.title') }}
      </p>
    </template>
  </v-card>
</template>

<style scoped lang="scss"></style>
