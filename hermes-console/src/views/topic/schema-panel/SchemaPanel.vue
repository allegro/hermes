<script setup lang="ts">
  import { computed, ref } from 'vue';
  import { copyToClipboard } from '@/utils/copy-utils';
  import AvroViewer from '@/views/topic/schema-panel/avro-viewer/AvroViewer.vue';
  import JsonViewer from '@/components/json-viewer/JsonViewer.vue';
  import type { ContentType } from '@/api/content-type';

  const props = defineProps<{
    schema: string;
    contentType: ContentType;
    topicName: string;
    schemaVersion?: number;
    availableSchemaVersions?: number[];
    schemaRegistryUrl?: string;
  }>();
  const showRawSchema = ref(false);
  const showVersionHistory = ref(false);

  const hasSchemaRegistryUrl = computed(
    () => !!props.schemaRegistryUrl?.trim(),
  );
  const sortedSchemaVersions = computed(() =>
    [...(props.availableSchemaVersions ?? [])].sort(
      (first, second) => second - first,
    ),
  );
  const shouldShowVersionHistory = computed(
    () =>
      props.contentType === 'AVRO' &&
      hasSchemaRegistryUrl.value &&
      sortedSchemaVersions.value.length > 0,
  );

  function schemaRegistryVersionUrl(version: number): string {
    const baseUrl = props.schemaRegistryUrl!.trim().replace(/\/+$/, '');
    return `${baseUrl}/subjects/${encodeURIComponent(props.topicName)}-value/versions/${version}`;
  }
</script>

<template>
  <div>
    <div class="d-flex justify-space-between mb-2">
      <v-btn-toggle
        v-model="showRawSchema"
        group
        density="compact"
        variant="outlined"
      >
        <v-btn :value="false" class="text-capitalize"
          >{{ $t('topicView.schema.structure') }}
        </v-btn>
        <v-btn :value="true" class="text-capitalize"
          >{{ $t('topicView.schema.rawSchema') }}
        </v-btn>
      </v-btn-toggle>
      <v-btn
        @click="copyToClipboard(props.schema)"
        flat
        variant="outlined"
        class="text-capitalize"
      >
        {{ $t('topicView.schema.copy') }}
      </v-btn>
    </div>
    <div class="mb-4" data-testid="schema-version-details">
      <template v-if="props.contentType === 'JSON'">
        {{ $t('topicView.schema.notApplicable') }}
      </template>
      <template v-else>
        <div v-if="props.schemaVersion !== undefined">
          {{
            $t('topicView.schema.activeVersion', {
              version: props.schemaVersion,
            })
          }}
        </div>
        <v-btn
          v-if="shouldShowVersionHistory"
          class="px-0 text-none"
          variant="text"
          @click="showVersionHistory = !showVersionHistory"
        >
          {{
            $t('topicView.schema.allVersions', {
              count: sortedSchemaVersions.length,
            })
          }}
        </v-btn>
        <ul
          v-if="showVersionHistory && shouldShowVersionHistory"
          data-testid="schema-version-history"
          class="schema-version-history"
        >
          <li v-for="version in sortedSchemaVersions" :key="version">
            <a
              :href="schemaRegistryVersionUrl(version)"
              target="_blank"
              rel="noopener noreferrer"
              >{{ version }}</a
            >
            <span v-if="version === props.schemaVersion" class="ml-2">
              {{ $t('topicView.schema.current') }}
            </span>
          </li>
        </ul>
      </template>
    </div>
    <div>
      <avro-viewer
        v-show="!showRawSchema"
        :schema="props.schema"
        data-testid="avro-viewer"
      />
      <v-card>
        <json-viewer
          v-show="showRawSchema"
          :json="props.schema"
          data-testid="json-viewer"
        />
      </v-card>
    </div>
  </div>
</template>

<style scoped>
  .schema-version-history {
    max-height: 320px;
    overflow-y: auto;
  }
</style>
