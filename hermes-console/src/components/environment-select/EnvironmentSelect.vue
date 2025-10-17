<script setup lang="ts">
  import type { ConsoleEnvironment } from '@/api/app-configuration';

  const props = defineProps<{
    isCurrentEnvironmentCritical?: boolean;
    knownEnvironments: ConsoleEnvironment[];
  }>();

  function getCurrentEnv(): ConsoleEnvironment {
    const url = location.href;
    const envs = props.knownEnvironments;
    return envs.find(
      (env) =>
        url.startsWith(env.url) ||
        url.startsWith(`http://${env.url}`) ||
        url.startsWith(`https://${env.url}`),
    );
  }

  function switchToEnv(env: ConsoleEnvironment, event): string {
    const currentUrl = location.href;
    const currentEnv: ConsoleEnvironment = getCurrentEnv();
    const switchedUrl = currentUrl.replace(currentEnv.url, env.url);
    if (event.ctrlKey || event.metaKey) {
      window.open(switchedUrl, '_blank');
    } else {
      window.location.replace(switchedUrl);
    }
  }
</script>

<template>
  <v-menu>
    <template v-slot:activator="{ props }">
      <v-btn
        :color="isCurrentEnvironmentCritical ? 'red-accent-2' : 'primary'"
        v-bind="props"
        block
        prepend-icon="mdi-server-outline"
        append-icon="mdi-unfold-more-horizontal"
        flat
        class="font-weight-bold justify-space-between"
      >
        <div>{{ getCurrentEnv().name }}</div>
      </v-btn>
    </template>

    <v-list class="pa-0">
      <v-list-item v-for="(item, i) in knownEnvironments" :key="i" :value="i">
        <v-list-item-title @click="(event) => switchToEnv(item, event)">{{
          item.name
        }}</v-list-item-title>
      </v-list-item>
    </v-list>
  </v-menu>
</template>

<style scoped lang="scss"></style>
