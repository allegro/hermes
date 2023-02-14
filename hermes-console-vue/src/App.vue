<template>
  <v-app class="fill-height">
    <console-header />

    <v-main class="main">
      <RouterView />
    </v-main>

    <console-footer />
  </v-app>
</template>

<script setup lang="ts">
  import { RouterView } from 'vue-router';
  import ConsoleFooter from '@/components/console-footer/ConsoleFooter.vue';
  import ConsoleHeader from '@/components/console-header/ConsoleHeader.vue';
</script>

<style scoped lang="scss">
  .main {
    margin: 0 auto;
    max-width: 1300px;
    width: 100%;
  }
</style>
