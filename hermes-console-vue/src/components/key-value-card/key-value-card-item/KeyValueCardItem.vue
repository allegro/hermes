<script setup lang="ts">
import TooltipIcon from "@/components/tooltip-icon/TooltipIcon.vue";

interface Props {
  name: string;
  value: any;
  tooltip?: string;
  href?: string;
}

const { name, value, tooltip, href } = defineProps<Props>();
</script>

<template>
  <tr>
    <th class="text-body-2 font-weight-light">
      <a v-if="href" :href="href">{{ name }}</a>
      <span v-else>{{ name }}</span>
    </th>
    <td class="text-body-2">
      <div class="d-flex">
        {{ value }}
        <tooltip-icon
          class="ml-auto"
          v-if="tooltip"
          :content="tooltip"
        />
      </div>
    </td>
  </tr>
</template>

<style scoped lang="scss"></style>
