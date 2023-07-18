<script setup lang="ts">
  import { RouterView } from 'vue-router';
  import ConsoleFooter from '@/components/console-footer/ConsoleFooter.vue';
  import ConsoleHeader from '@/components/console-header/ConsoleHeader.vue';
  import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
  import LoadingSpinner from '@/components/loading-spinner/LoadingSpinner.vue';
  import ConsoleAlert from '@/components/console-alert/ConsoleAlert.vue';
  import { useTheme } from 'vuetify';
  import ThemeSwitch from '@/components/theme-switch/ThemeSwitch.vue';

  const configStore = useAppConfigStore();
  const theme = useTheme();
  theme.current.value.dark;
  configStore.loadConfig();
</script>

<template>
  <v-app class="fill-height">
    <template v-if="configStore.loading">
      <v-main class="main">
        <loading-spinner />
      </v-main>
    </template>

    <template v-else-if="configStore.error.loadConfig && !configStore.loading">
      <!--            <console-header />-->

      <v-main class="main">
        <RouterView />
      </v-main>
    </template>

    <template v-else>
      <console-header />

      <v-main class="main">
        <RouterView />
      </v-main>

      <console-footer />
    </template>
  </v-app>
</template>

<style scoped lang="scss">
  .main {
    margin: 0 auto;
    max-width: 1300px;
    width: 100%;
  }
</style>
