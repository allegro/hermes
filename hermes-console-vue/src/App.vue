<template>
  <v-app class="fill-height">
    <hermes-header />

    <v-main>
      <RouterView />
    </v-main>

    <hermes-footer />
  </v-app>
</template>

<script setup lang="ts">
  import { RouterView } from 'vue-router';
  import HermesFooter from '@/components/common/hermes-footer/HermesFooter.vue';
  import HermesHeader from '@/components/common/hermes-header/HermesHeader.vue';
</script>

<style scoped lang="scss"></style>
