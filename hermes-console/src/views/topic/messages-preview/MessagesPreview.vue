<script setup lang="ts">
  import { formatTimestampMillis } from '@/utils/date-formatter/date-formatter';
  import { ref } from 'vue';
  import JsonViewer from '@/components/json-viewer/JsonViewer.vue';
  import type { MessagePreview } from '@/api/topic';
  import type {
    ParsedMessagePreview,
    SelectedRow,
  } from '@/views/topic/messages-preview/types';

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

  const selectedMessage = ref<ParsedMessagePreview | null>(null);
  const dialog = ref(false);

  function openMessageDialog(message: ParsedMessagePreview) {
    selectedMessage.value = message;
    dialog.value = true;
    console.log(message);
  }

  function closeMessageDialog() {
    dialog.value = false;
    selectedMessage.value = null;
  }

  // TODO: Handle truncated messages properly
</script>

<template>
  <v-card>
    <template #title>
      <div class="d-flex justify-space-between">
        <p class="font-weight-bold">Messages Preview</p>
      </div>
    </template>
    <v-card-text>
      <v-data-table
        :items="parsedMessages"
        :headers="messagesTableHeaders"
        hover
        @click:row="
          (_: Event, row: SelectedRow<ParsedMessagePreview>) =>
            openMessageDialog(row.item)
        "
      >
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

  <v-dialog
    v-model="dialog"
    min-width="800"
    max-height="800"
    v-if="selectedMessage"
  >
    <v-card>
      <v-card-item class="border-b">
        <div class="d-flex justify-space-between align-start">
          <div>
            <v-card-title>Message details</v-card-title>
            <v-card-subtitle>
              Inspect the event payload and metadata.
            </v-card-subtitle>
          </div>
          <v-btn icon="mdi-close" variant="text" @click="closeMessageDialog" />
        </div>
      </v-card-item>

      <v-card-text class="pt-4 d-flex flex-column row-gap-4">
        <div class="d-flex flex-column">
          <span class="text-body-2 text-medium-emphasis">Message ID</span>
          <span>{{ selectedMessage.parsedContent.__metadata?.messageId }}</span>
        </div>
        <div class="d-flex flex-column">
          <span class="text-body-2 text-medium-emphasis">Timestamp</span>
          <span>{{
            formatTimestampMillis(
              Number(selectedMessage.parsedContent.__metadata?.timestamp),
            )
          }}</span>
        </div>
        <div>
          <span class="text-body-2 text-medium-emphasis">Payload</span>
          <json-viewer
            :json="selectedMessage.content"
            class="border-thin rounded-lg"
          />
        </div>
      </v-card-text>
    </v-card>
  </v-dialog>
</template>

<style scoped lang="scss">
  .raw-schema-snippet {
    line-height: 1.4;
    max-height: 200px;
    overflow: scroll;
    border: #cccccc 1px solid;
  }
</style>
