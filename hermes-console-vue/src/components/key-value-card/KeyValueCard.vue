<script setup lang="ts">
  const props = defineProps<{
    title: string;
  }>();
</script>

<template>
  <v-card>
    <template #title>
      <p class="font-weight-bold">
        {{ props.title }}
      </p>
    </template>
    <v-table density="compact">
      <tbody>
        <slot></slot>
      </tbody>
    </v-table>
  </v-card>
</template>

<style scoped lang="scss"></style>
