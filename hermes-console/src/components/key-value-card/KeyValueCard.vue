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
        <v-card-actions v-if="buttonHref">
          <v-btn
            class="text-none"
            prepend-icon="mdi-open-in-new"
            target="_blank"
            :href="buttonHref!!"
          >
            {{ props.buttonText }}
          </v-btn>
        </v-card-actions>
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
