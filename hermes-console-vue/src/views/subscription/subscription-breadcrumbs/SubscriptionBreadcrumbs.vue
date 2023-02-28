<script setup lang="ts">
  import { useI18n } from 'vue-i18n';

  const props = defineProps<{
    groupId: string;
    topicId: string;
    subscriptionId: string;
  }>();

  const { t } = useI18n();

  const items = [
    {
      title: t('subscription.subscriptionBreadcrumbs.home'),
      href: '/',
    },
    {
      title: t('subscription.subscriptionBreadcrumbs.groups'),
      href: '/groups/',
    },
    {
      title: props.groupId,
      href: `/groups/${props.groupId}/`,
    },
    {
      title: props.topicId,
      href: `/groups/${props.groupId}/topics/${props.topicId}/`,
    },
    {
      title: props.subscriptionId,
    },
  ];
</script>

<template>
  <v-breadcrumbs :items="items" density="compact" />
</template>

<style scoped lang="scss"></style>
