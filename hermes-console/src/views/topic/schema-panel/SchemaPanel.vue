<script setup lang="ts">
  import { copyToClipboard } from '@/utils/copy-utils';
  import { defineProps, ref } from 'vue';
  import AvroViewer from '@/views/topic/schema-panel/avro-viewer/AvroViewer.vue';
  import JsonViewer from '@/components/json-viewer/JsonViewer.vue';

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
        <v-btn :value="false" class="text-capitalize">{{
          $t('topicView.schema.structure')
        }}</v-btn>
        <v-btn :value="true" class="text-capitalize">{{
          $t('topicView.schema.rawSchema')
        }}</v-btn>
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
        <json-viewer v-show="showRawSchema" :json="props.schema" />
      </v-card>
    </div>
  </div>
</template>
