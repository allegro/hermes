<script setup lang="ts">
  import { copyToClipboard } from '@/utils/copy-utils';
  import { defineProps, ref } from 'vue';
  import AvroViewer from '@/views/topic/schema-panel/avro-viewer/AvroViewer.vue';
  const props = defineProps<{
    schema: string;
  }>();
  const showRawSchema = ref(false);
</script>

<template>
  <v-expansion-panels>
    <v-expansion-panel :title="$t('topicView.schema.title')">
      <v-expansion-panel-text>
        <div class="d-flex">
          <v-checkbox
            v-model="showRawSchema"
            :label="$t('topicView.schema.showRawSchema')"
            class="text-right"
          ></v-checkbox>
          <v-btn @click="copyToClipboard(props.schema)" class="mt-2">
            {{ $t('topicView.schema.copy') }}
          </v-btn>
        </div>
        <AvroViewer v-show="!showRawSchema" :schema="props.schema" />
        <pre v-show="showRawSchema">
          <v-code class="raw-schema-snippet">{{ JSON.parse(props.schema) }}</v-code>
        </pre>
      </v-expansion-panel-text>
    </v-expansion-panel>
  </v-expansion-panels>
</template>

<style scoped lang="scss">
  .raw-schema-snippet {
    line-height: 1.4;
    max-height: 500px;
    overflow: scroll;
    border: #cccccc 1px solid;
  }
</style>
