<script setup lang="ts">
  const {
    title,
    subtitle,
    icon,
    label,
    labelColor = 'primary',
  } = defineProps<{
    title: string;
    subtitle?: string;
    icon?: string;
    label?: string;
    labelColor?: string;
  }>();

  const emit = defineEmits(['click']);
</script>

<template>
  <v-list-item role="button" @click="emit('click')">
    <template v-if="icon" #prepend>
      <v-icon :icon="icon" class="mr-2" />
    </template>

    <v-list-item-title>{{ title }}</v-list-item-title>
    <v-list-item-subtitle v-if="subtitle">{{ subtitle }}</v-list-item-subtitle>

    <template #append>
      <v-chip size="small" :color="labelColor" text-color="white">
        {{ label }}
      </v-chip>
    </template>
  </v-list-item>
</template>

<style scoped lang="scss"></style>
