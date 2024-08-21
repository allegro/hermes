<script setup lang="ts">
  import { useI18n } from 'vue-i18n';
  import type { InconsistentMedata } from '@/api/inconsistent-group';

  const props = defineProps<{
    metadata: InconsistentMedata[];
  }>();

  const { t } = useI18n();

  const emit = defineEmits<{
    sync: [datacenter: string];
  }>();

  const prettyJson = (jsonString: string | undefined): string => {
    if (jsonString === undefined) {
      return 'null';
    } else {
      return JSON.parse(jsonString);
    }
  };

  function sync(datacenter: string) {
    emit('sync', datacenter);
  }
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
    <v-card-text>
      <p class="text-h5">{{ $t('consistency.sync.header') }}</p>
      <p class="text-subtitle-1 mt-2">
        {{ $t('consistency.sync.explanation') }}
      </p>
    </v-card-text>
    <v-card-actions>
      <p class="text mt-2 mr-4 ml-2">{{ $t('consistency.sync.cta') }}</p>
      <v-btn
        class="bg-primary"
        v-for="meta in metadata"
        @click="sync(meta.datacenter)"
        :key="meta.datacenter"
        :data-testid="`sync-datacenter-${meta.datacenter}`"
      >
        {{ meta.datacenter }}
      </v-btn>
    </v-card-actions>
  </v-card>
  <v-banner
    v-else
    style="background-color: #c8e6c9"
    :text="t('consistency.inconsistentGroup.metadata.consistent')"
  >
  </v-banner>
</template>
