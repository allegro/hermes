<script setup lang="ts">
  import TooltipIcon from '@/components/tooltip-icon/TooltipIcon.vue';

  const props = defineProps<{
    name: string;
    value: any;
    tooltip?: string;
    href?: string;
  }>();
</script>

<template>
  <tr>
    <th class="text-body-2 font-weight-light">
      <a v-if="href" :href="href">{{ props.name }}</a>
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
