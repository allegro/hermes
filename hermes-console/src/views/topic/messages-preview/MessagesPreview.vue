<script setup lang="ts">
  import { formatTimestampMillis } from '@/utils/date-formatter/date-formatter';
  import type { MessagePreview } from '@/api/topic';
  import type { ParsedMessagePreview } from '@/views/topic/messages-preview/types';

  const props = defineProps<{
    messages: MessagePreview[];
  }>();

  const parsedMessages = props.messages.map(
    (message) =>
      ({
        ...message,
        parsedContent: JSON.parse(message.content || '{}'),
      }) satisfies ParsedMessagePreview,
  );

  const messagesTableHeaders = [
    { title: 'Message Id', key: 'messageId' },
    { title: 'Timestamp', key: 'timestamp' },
    { title: 'Message', key: 'content' },
    { title: 'Truncated', key: 'truncated' },
  ];
</script>

<template>
  <v-card>
    <template #title>
      <div class="d-flex justify-space-between">
        <p class="font-weight-bold">Messages Preview</p>
      </div>
    </template>

    <v-card-text>
      <v-data-table :items="parsedMessages" :headers="messagesTableHeaders">
        <template #[`item.messageId`]="{ item }">
          {{ item?.parsedContent?.__metadata?.messageId }}
        </template>
        <template #[`item.timestamp`]="{ item }">
          {{
            formatTimestampMillis(
              Number(item?.parsedContent?.__metadata?.timestamp),
            )
          }}
        </template>
      </v-data-table>
    </v-card-text>
  </v-card>
</template>

<style scoped lang="scss">
  .raw-schema-snippet {
    line-height: 1.4;
    max-height: 200px;
    overflow: scroll;
    border: #cccccc 1px solid;
  }
</style>
