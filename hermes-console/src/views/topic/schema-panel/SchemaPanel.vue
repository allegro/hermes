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
    schemaSubject: string;
    schemaRegistryUrl: string;
  }>();
  const showRawSchema = ref(false);

  const sortedSchemaVersions = computed(() =>
    [...(props.availableSchemaVersions ?? [])].sort(
      (first, second) => second - first,
    ),
  );
  const shouldShowVersionHistory = computed(
    () => props.contentType === 'AVRO' && sortedSchemaVersions.value.length > 0,
  );

  function schemaRegistryVersionUrl(version: number): string {
    const baseUrl = props.schemaRegistryUrl.trim().replace(/\/+$/, '');
    return `${baseUrl}/subjects/${encodeURIComponent(props.schemaSubject)}/versions/${version}`;
  }
</script>

<template>
  <div class="pt-6">
    <div class="mb-4" data-testid="schema-version-details">
      <template v-if="props.contentType === 'JSON'">
        {{ $t('topicView.schema.notApplicable') }}
      </template>
      <template v-else>
        <div class="d-flex align-center ga-2">
          <div v-if="props.schemaVersion !== undefined">
            {{ $t('topicView.schema.activeVersion') }}
            <strong>{{ props.schemaVersion }}</strong>
          </div>
          <v-menu v-if="shouldShowVersionHistory" location="bottom start">
            <template #activator="{ props: menuProps }">
              <v-btn
                v-bind="menuProps"
                append-icon="mdi-chevron-down"
                class="text-none"
                variant="outlined"
              >
                {{
                  $t('topicView.schema.allVersions', {
                    count: sortedSchemaVersions.length,
                  })
                }}
              </v-btn>
            </template>
            <v-list
              data-testid="schema-version-history"
              class="schema-version-history"
            >
              <v-list-item
                v-for="version in sortedSchemaVersions"
                :key="version"
                :href="schemaRegistryVersionUrl(version)"
                target="_blank"
                rel="noopener noreferrer"
              >
                <v-list-item-title>
                  {{ version }}
                  <span v-if="version === props.schemaVersion" class="ml-2">
                    {{ $t('topicView.schema.current') }}
                  </span>
                </v-list-item-title>
              </v-list-item>
            </v-list>
          </v-menu>
        </div>
      </template>
    </div>
    <div class="d-flex justify-space-between mt-6 mb-2">
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
    min-width: 160px;
  }
</style>
