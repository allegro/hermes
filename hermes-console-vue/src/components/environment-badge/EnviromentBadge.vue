<script setup lang="ts">
  const props = defineProps<{
    environmentName: string;
    isCriticalEnvironment: boolean;
  }>();

  function environmentBadgeColor(): string {
    if (props.isCriticalEnvironment) {
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
