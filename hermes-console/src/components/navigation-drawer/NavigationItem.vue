<script setup lang="ts">
  import { defineProps } from 'vue';
  import { useRouter } from 'vue-router';
  const router = useRouter();

  const props = defineProps<{
    icon?: string;
    translationKey: string;
    name: string;
    currentRouteName?: string;
    readonly?: boolean;
    externalUrl?: string;
  }>();

  function navigateToRoute() {
    if (props.readonly || props.externalUrl) {
      return;
    }
    router.push({ name: props.name });
  }
</script>

<template>
  <v-list-item
    :prepend-icon="icon"
    :title="!externalUrl ? $t(translationKey) : ''"
    :active="currentRouteName === name"
    :value="name"
    @click="navigateToRoute"
  >
    <a v-if="externalUrl" :href="externalUrl" target="_blank">
      <v-list-item-title>{{ $t(translationKey) }}</v-list-item-title>
    </a>
  </v-list-item>
</template>

<style scoped lang="scss">
  a {
    text-decoration: none;
    color: inherit;
  }
</style>
