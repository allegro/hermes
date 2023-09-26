<script setup lang="ts">
  import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';
  import type { Notification } from '@/store/app-notifications/types';
  const props = defineProps<{
    notification: Notification;
  }>();
  const emit = defineEmits<{
    close: [];
  }>();

  const notificationsStore = useNotificationsStore();

  if (!props.notification.persistent) {
    setTimeout(
      () => {
        notificationsStore.removeNotification(props.notification.id);
      },
      props.notification.duration
        ? props.notification.duration
        : calculateDefaultTimeout(),
    );
  }

  function calculateDefaultTimeout(): number {
    return props.notification.type === 'error' ? 15000 : 10000;
  }
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
    @click:close="emit('close')"
  />
</template>
<style scoped lang="scss"></style>
