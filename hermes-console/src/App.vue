<script setup lang="ts">
  import { RouterView } from 'vue-router';
  import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
  import AppNotificationProvider from '@/components/app-notification/AppNotificationProvider.vue';
  import ConsoleFooter from '@/components/console-footer/ConsoleFooter.vue';
  import ConsoleHeader from '@/components/console-header/ConsoleHeader.vue';

  const configStore = useAppConfigStore();
  configStore.loadConfig();
</script>

<template>
  <v-app class="fill-height">
    <div v-if="configStore.loadedConfig">
      <console-header />

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
    max-width: 1300px;
    width: 100%;
  }
</style>
