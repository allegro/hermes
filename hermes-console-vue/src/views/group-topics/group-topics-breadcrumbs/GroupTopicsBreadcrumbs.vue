<script setup lang="ts">
  import { useI18n } from 'vue-i18n';

  const props = defineProps<{
    groupId: string;
  }>();

  const { t } = useI18n();

  const items = [
    {
      title: t('groupTopics.groupTopicsBreadcrumbs.home'),
      href: '/',
    },
    {
      title: t('groupTopics.groupTopicsBreadcrumbs.groups'),
      href: '/groups/',
    },
    {
      title: props.groupId,
      href: `/groups/${props.groupId}/`,
    },
  ];
</script>

<template>
  <v-breadcrumbs :items="items" density="compact" />
</template>

<style scoped lang="scss"></style>
