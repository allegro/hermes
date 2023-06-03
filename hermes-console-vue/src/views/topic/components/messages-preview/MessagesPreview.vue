<script setup lang="ts">
  import type { MessagePreview } from '@/api/topic';
  const props = defineProps<{
    messages: MessagePreview[];
  }>();
</script>

<template>
  <v-expansion-panels>
    <v-expansion-panel title="Topic messages preview">
      <v-expansion-panel-text>
        <pre v-for="(message, index) in messages" :key="index">
          <v-code class="raw-schema-snippet">{{ JSON.parse(message.content) }}</v-code>
        </pre>
      </v-expansion-panel-text>
    </v-expansion-panel>
  </v-expansion-panels>
</template>

<style scoped lang="scss">
  .raw-schema-snippet {
    line-height: 1.4;
    max-height: 200px;
    overflow-y: scroll;
    overflow-x: scroll;
    border: #cccccc 1px solid;
  }
</style>
