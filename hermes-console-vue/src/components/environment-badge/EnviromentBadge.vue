<script setup lang="ts">
  interface HermesEnvironmentProps {
    environmentName: string;
  }

  const props = defineProps<HermesEnvironmentProps>();

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
