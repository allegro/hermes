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
      title: t('consumerGroups.breadcrumbs.home'),
      href: '/',
    },
    {
      title: t('consumerGroups.breadcrumbs.groups'),
      href: '/groups',
    },
    {
      title: props.groupId,
      href: `/groups/${props.groupId}`,
    },
    {
      title: props.topicId,
      href: `/groups/${props.groupId}/topics/${props.topicId}`,
    },
    {
      title: props.subscriptionId,
      href: `/groups/${props.groupId}/topics/${props.topicId}/subscriptions/${props.subscriptionId}`,
    },
    {
      title: t('consumerGroups.breadcrumbs.title'),
    },
  ];
</script>

<template>
  <v-breadcrumbs :items="items" density="compact" />
</template>

<style scoped lang="scss"></style>
