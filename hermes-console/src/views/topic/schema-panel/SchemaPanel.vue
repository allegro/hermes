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
  <div>
    <div class="d-flex justify-space-between mb-2">
      <v-btn-toggle
        v-model="showRawSchema"
        group
        density="compact"
        variant="outlined"
      >
        <v-btn :value="false" class="text-capitalize">Structure</v-btn>
        <v-btn :value="true" class="text-capitalize">Raw schema</v-btn>
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
      <AvroViewer v-show="!showRawSchema" :schema="props.schema" />
      <v-card>
        <pre v-show="showRawSchema">
          <v-code class="raw-schema-snippet">{{ JSON.parse(props.schema) }}</v-code>
        </pre>
      </v-card>
    </div>
  </div>
</template>

<style scoped lang="scss">
  .raw-schema-snippet {
    line-height: 1.4;
    max-height: 500px;
    overflow: scroll;
    //border: #cccccc 1px solid;
  }
</style>
