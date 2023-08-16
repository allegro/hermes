<script setup lang="ts">
  import { useAppNotifications } from '@/store/app-notifications/useAppNotifications';
  import AppNotification from '@/components/app-notification/AppNotification.vue';

  const notificationsStore = useAppNotifications();
</script>

<template>
  <div
    class="position-fixed d-flex justify-start align-end app-notifications-provider"
  >
    <TransitionGroup
      appear
      tag="div"
      class="app-notifications-provider__notifications-container"
    >
      <AppNotification
        v-for="notification in notificationsStore.notifications"
        :key="notification.id"
        :notification="notification"
        class="app-notifications-provider__notification"
      />
    </TransitionGroup>
  </div>
  <slot />
</template>

<style scoped lang="scss">
  .app-notifications-provider {
    inset: 0;
    padding: 32px 32px;
    //z-index: 3000;

    &__notification {
      z-index: 3000;
    }

    //&__notifications-container {
    //  display: flex;
    //  flex-direction: column;
    //  row-gap: 8px;
    //}
  }
</style>
