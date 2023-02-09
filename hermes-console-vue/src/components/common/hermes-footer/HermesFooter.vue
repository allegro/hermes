<script setup lang="ts">
  const links: { label: string; href: string }[] = [
    { label: 'read the docs', href: 'https://hermes-pubsub.readthedocs.io/' },
    { label: 'contact', href: '/' },
  ];
</script>

<template>
  <v-footer app class="bg-grey-lighten-4">
    <v-row justify="center" no-gutters>
      <v-btn
        v-for="link in links"
        :key="link"
        variant="text"
        class="mx-2"
        rounded="xl"
        :href="link.href"
      >
        {{ link.label }}
      </v-btn>
      <v-col class="text-center text-caption my-1" cols="12">
        2015–{{ new Date().getFullYear() }} Allegro Tech and contributors
      </v-col>
    </v-row>
  </v-footer>
</template>

<style scoped></style>
