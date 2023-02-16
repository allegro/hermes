<script setup lang="ts">
  import { computed } from 'vue';
  import TooltipIcon from '@/components/tooltip-icon/TooltipIcon.vue';

  interface KeyValueEntry {
    displayIf?: boolean;
    name: string;
    nameHref?: string;
    value?: string | number | boolean;
    tooltip?: string;
  }

  interface KeyValueCardProps {
    cardTitle: string;
    entries: KeyValueEntry[];
  }

  const props = defineProps<KeyValueCardProps>();

  // TODO: test filtering
  const filteredEntries = computed(() =>
    props.entries.filter((entry) => entry.displayIf !== false),
  );
</script>

<template>
  <v-card class="mb-2">
    <template #title>
      <p class="font-weight-bold">
        {{ props.cardTitle }}
      </p>
    </template>
    <v-table density="compact">
      <tbody>
        <tr v-for="entry in filteredEntries" :key="entry.name">
          <th class="text-body-2 font-weight-light">
            <component
              :is="entry.nameHref ? 'a' : 'span'"
              :href="entry.nameHref"
            >
              {{ entry.name }}
            </component>
          </th>
          <td class="text-body-2">
            <div class="d-flex">
              {{ entry.value }}
              <tooltip-icon
                class="ml-auto"
                v-if="entry.tooltip"
                :content="entry.tooltip"
              />
            </div>
          </td>
        </tr>
      </tbody>
    </v-table>
  </v-card>
</template>

<style scoped lang="scss"></style>
