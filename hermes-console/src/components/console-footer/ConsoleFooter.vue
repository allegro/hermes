<script setup lang="ts">
  import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';

  const configStore = useAppConfigStore();

  const links: { label: string; href: string }[] = [
    { label: 'read the docs', href: 'https://hermes-pubsub.readthedocs.io/' },
    {
      label: 'contact',
      href: `${configStore.loadedConfig.console.contactLink}`,
    },
  ];
</script>

<template>
  <v-footer app absolute>
    <v-row justify="center" no-gutters>
      <v-btn
        v-for="link in links"
        :key="link.href"
        variant="text"
        class="mx-2"
        rounded="xl"
        target="_blank"
        :href="link.href"
      >
        {{ link.label }}
      </v-btn>
      <v-col class="text-center text-caption my-1" cols="12">
        2015–{{ new Date().getFullYear() }} &#10084; Allegro Tech & contributors
      </v-col>
    </v-row>
  </v-footer>
</template>

<style scoped></style>
