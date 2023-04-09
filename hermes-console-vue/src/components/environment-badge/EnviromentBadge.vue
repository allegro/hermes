<script setup lang="ts">
  const props = defineProps<{
    environmentName: string;
  }>();

  function environmentBadgeColor(): string {
    if (props.environmentName.toLowerCase().startsWith('prod')) {
      return 'red';
    }
    return 'blue';
  }
</script>

<template>
  <v-chip :color="environmentBadgeColor()" size="small">
    {{ environmentName.toUpperCase() }}
  </v-chip>
</template>

<style scoped lang="scss"></style>
