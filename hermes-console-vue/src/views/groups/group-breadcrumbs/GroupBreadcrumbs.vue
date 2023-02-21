<script setup lang="ts">
  import { useI18n } from 'vue-i18n';

  const { t } = useI18n();

  const items = [
    {
      title: t('groups.groupBreadcrumbs.home'),
      href: '/',
    },
    {
      title: t('groups.groupBreadcrumbs.groups'),
    },
  ];
</script>

<template>
  <v-breadcrumbs :items="items" density="compact" />
</template>

<style scoped lang="scss"></style>
