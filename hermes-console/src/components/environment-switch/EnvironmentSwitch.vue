<script setup lang="ts">
  import { computed } from 'vue';
  import type { ConsoleEnvironment } from '@/api/app-configuration';

  const props = defineProps<{
    knownEnvironments: ConsoleEnvironment[];
  }>();

  const selectedEnv = computed(() =>
    props.knownEnvironments.indexOf(getCurrentEnv()),
  );

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
  <v-btn-toggle color="primary" rounded="0" v-model="selectedEnv" group>
    <v-btn
      v-for="env in knownEnvironments"
      min-width="100"
      v-bind:key="env.name"
      @click="switchToEnv(env, $event)"
    >
      {{ env.name }}
    </v-btn>
  </v-btn-toggle>
</template>

<style scoped lang="scss"></style>
