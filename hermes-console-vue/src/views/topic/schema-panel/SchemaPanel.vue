<script setup lang="ts">
  import { copyToClipboard } from '@/utils/copy-utils';

  const props = defineProps<{
    schema: string;
  }>();
</script>

<template>
  <v-expansion-panels>
    <v-expansion-panel :title="$t('topicView.schema.title')">
      <v-expansion-panel-text>
        <v-btn @click="copyToClipboard(props.schema)">{{
          $t('topicView.schema.copy')
        }}</v-btn>
        <pre>
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
