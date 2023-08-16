<script setup lang="ts">
  import type { Notification } from '@/store/app-notifications/types';

  const props = defineProps<{
    notification: Notification;
  }>();
</script>

<template>
  <v-alert
    :title="notification.title ?? ''"
    :text="notification.text"
    :type="props.notification.type"
    :closable="true"
    border="start"
    elevation="3"
    width="500"
  />
</template>

<style scoped lang="scss"></style>
