<script setup lang="ts">
  import { RouterView } from 'vue-router';
  import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
  import AppNotificationProvider from '@/components/app-notification/AppNotificationProvider.vue';
  import ConsoleFooter from '@/components/console-footer/ConsoleFooter.vue';
  import ConsoleHeader from '@/components/console-header/ConsoleHeader.vue';
  import NavigationDrawer from '@/components/navigation-drawer/NavigationDrawer.vue';

  const configStore = useAppConfigStore();
  configStore.loadConfig();
</script>

<template>
  <v-app class="fill-height">
    <div v-if="configStore.loadedConfig">
      <console-header />
      <navigation-drawer v-if="$route.name && $route.name !== 'home'" />
      <v-main class="main">
        <app-notification-provider>
          <router-view />
        </app-notification-provider>
      </v-main>

      <console-footer />
    </div>
  </v-app>
</template>

<style scoped lang="scss">
  .main {
    margin: 0 auto;
    max-width: 1800px;
    width: 100%;
  }
</style>
