<script setup lang="ts">

interface Props {
  title: string;
}

const { title } = defineProps<Props>();

</script>

<template>
  <v-card>
    <template #title>
      <p class="font-weight-bold">
        {{ title }}
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
