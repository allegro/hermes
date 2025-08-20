<script setup lang="ts">
  const props = defineProps<{
    title: string;
    buttonText?: string;
    buttonHref?: string | null;
  }>();
</script>

<template>
  <v-card>
    <template #title>
      <div class="d-flex justify-space-between">
        <p class="font-weight-bold">
          {{ props.title }}
        </p>
        <v-btn
          v-if="buttonText"
          class="text-capitalize"
          prepend-icon="mdi-open-in-new"
          target="_blank"
          :href="buttonHref!!"
          variant="text"
          color="primary"
        >
          {{ props.buttonText }}
        </v-btn>
      </div>
    </template>
    <v-table density="compact">
      <tbody>
        <slot></slot>
      </tbody>
    </v-table>
  </v-card>
</template>

<style scoped lang="scss"></style>
