<script setup lang="ts">
  interface SubscriptionBreadcrumbsProps {
    groupId: string;
    topicId: string;
    subscriptionId: string;
  }

  const props = defineProps<SubscriptionBreadcrumbsProps>();

  const items = [
    {
      title: 'home',
      href: '/',
    },
    {
      title: 'groups',
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
    },
  ];
</script>

<template>
  <v-breadcrumbs :items="items" density="compact" />
</template>

<style scoped lang="scss"></style>
