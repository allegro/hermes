<script setup lang="ts">
  import { useI18n } from 'vue-i18n';
  import type { InconsistentMedata } from '@/api/inconsistent-group';

  const props = defineProps<{
    metadata: InconsistentMedata[];
  }>();

  const { t } = useI18n();

  const prettyJson = (jsonString: string | undefined): string => {
    if (jsonString === undefined) {
      return 'null';
    } else {
      return JSON.parse(jsonString);
    }
  };
</script>

<template>
  <v-card
    v-if="metadata.length > 0"
    :title="t('consistency.inconsistentGroup.metadata.inconsistent')"
  >
    <v-table>
      <thead>
        <tr>
          <th v-for="meta in props.metadata" :key="meta">
            {{ meta.datacenter }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td v-for="meta in props.metadata" :key="meta">
            <code>
              {{ prettyJson(meta.content) }}
            </code>
          </td>
        </tr>
      </tbody>
    </v-table>
  </v-card>
  <v-banner
    v-else
    style="background-color: #c8e6c9"
    :text="t('consistency.inconsistentGroup.metadata.consistent')"
  >
  </v-banner>
</template>
