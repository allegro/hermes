<script setup lang="ts">
  const props = defineProps<{
    title: string;
    buttonText?: string;
    buttonHref?: string | null;
  }>();
</script>

<template>
  <v-card>
    <v-card-item class="border-b">
      <div class="d-flex justify-space-between align-start">
        <v-card-title class="font-weight-bold">
          {{ props.title }}
        </v-card-title>
        <div class="d-flex justify-space-between">
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
      </div>
    </v-card-item>
    <v-card-item>
      <v-table density="compact">
        <tbody>
          <slot></slot>
        </tbody>
      </v-table>
    </v-card-item>
  </v-card>
</template>

<style scoped lang="scss"></style>
