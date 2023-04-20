<script setup lang="ts">
  import { useI18n } from 'vue-i18n';

  const { t } = useI18n();

  const items = [
    {
      title: t('readiness.breadcrumbs.home'),
      href: '/',
    },
    {
      title: t('readiness.breadcrumbs.title'),
    },
  ];
</script>

<template>
  <v-breadcrumbs :items="items" density="compact" />
</template>

<style scoped lang="scss"></style>
