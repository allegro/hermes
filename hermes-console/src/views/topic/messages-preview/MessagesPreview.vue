<script setup lang="ts">
  import type { MessagePreview } from '@/api/topic';
  const props = defineProps<{
    messages: MessagePreview[];
  }>();
</script>

<template>
  <v-expansion-panels>
    <v-expansion-panel :title="$t('topicView.messagesPreview.title')">
      <v-expansion-panel-text>
        <div class="d-flex flex-column row-gap-2">
          <pre
            v-for="(message, index) in props.messages"
            :key="index"
          ><v-code class="raw-schema-snippet">{{ JSON.parse(message.content) }}</v-code></pre>
        </div>
      </v-expansion-panel-text>
    </v-expansion-panel>
  </v-expansion-panels>
</template>

<style scoped lang="scss">
  .raw-schema-snippet {
    line-height: 1.4;
    max-height: 200px;
    overflow: scroll;
    border: #cccccc 1px solid;
  }
</style>
