<template>
  <v-app class="fill-height">
    <console-header />

    <v-main>
      <RouterView />
    </v-main>

    <console-footer />
  </v-app>
</template>

<script setup lang="ts">
  import { RouterView } from 'vue-router';
  import ConsoleFooter from '@/components/common/console-footer/ConsoleFooter.vue';
  import ConsoleHeader from '@/components/common/console-header/ConsoleHeader.vue';
</script>

<style scoped lang="scss"></style>
