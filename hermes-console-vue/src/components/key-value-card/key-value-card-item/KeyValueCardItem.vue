<script setup lang="ts">
  import TooltipIcon from '@/components/tooltip-icon/TooltipIcon.vue';

  const props = defineProps<{
    name: string;
    nameHref?: string;
    value?: any;
    tooltip?: string;
  }>();
</script>

<template>
  <tr>
    <th class="text-body-2 font-weight-light">
      <a v-if="nameHref" :href="nameHref">{{ props.name }}</a>
      <span v-else>{{ props.name }}</span>
    </th>
    <td class="text-body-2">
      <div class="d-flex">
        {{ props.value }}
        <tooltip-icon
          class="ml-auto"
          v-if="props.tooltip"
          :content="props.tooltip"
        />
      </div>
    </td>
  </tr>
</template>

<style scoped lang="scss"></style>
