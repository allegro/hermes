<script setup lang="ts">
  interface TooltipIconProps {
    content: string;
  }
  const props = defineProps<TooltipIconProps>();
</script>

<template>
  <div class="tooltip__icon">
    <v-tooltip :text="props.content">
      <template v-slot:activator="{ props }">
        <v-icon
          v-bind="props"
          icon="mdi-help-circle-outline"
          color="grey-lighten-1"
          size="18px"
        />
      </template>
    </v-tooltip>
  </div>
</template>

<style scoped lang="scss">
  .tooltip {
    &__icon {
      cursor: pointer;
    }
  }
</style>
