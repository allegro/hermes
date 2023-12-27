<script setup lang="ts">
  import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';
  import AppNotification from '@/components/app-notification/AppNotification.vue';
  const notificationsStore = useNotificationsStore();
</script>

<template>
  <div
    class="position-fixed d-flex justify-start align-end app-notifications-provider"
  >
    <transition-group
      appear
      tag="div"
      class="app-notifications-provider__notifications-container d-flex flex-column"
    >
      <app-notification
        v-for="notification in notificationsStore.notifications"
        :key="notification.id"
        :notification="notification"
        class="app-notifications-provider__notification"
        @close="notificationsStore.removeNotification(notification.id)"
      />
    </transition-group>
  </div>
  <slot />
</template>

<style scoped lang="scss">
  .app-notifications-provider {
    z-index: 3000;
    bottom: 32px;
    left: 32px;
    &__notification {
      z-index: 2000;
    }
    &__notifications-container {
      row-gap: 16px;
    }
  }
</style>
