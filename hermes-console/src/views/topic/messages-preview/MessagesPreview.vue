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

  const parsedMessages = props.messages.map((message) => {
    let parsedContent: any | null = null;
    try {
      if (!message.truncated) {
        parsedContent = JSON.parse(message.content || '{}');
      }
    } catch (e) {
      /* empty */
    }

    return {
      ...message,
      messageId: parsedContent?.__metadata?.messageId || 'Not available',
      timestamp: parsedContent?.__metadata?.timestamp
        ? Number(parsedContent?.__metadata?.timestamp)
        : null,
      parsedContent,
    };
  });

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
          {{ item?.messageId || 'Not available' }}
        </template>
        <template #[`item.timestamp`]="{ item }">
          {{
            item?.timestamp
              ? formatTimestampMillis(item?.timestamp)
              : 'Not available'
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
          <span>{{ selectedMessage.messageId || 'Not available' }}</span>
        </div>
        <div class="d-flex flex-column">
          <span class="text-body-2 text-medium-emphasis">Timestamp</span>
          <span>{{
            selectedMessage?.timestamp
              ? formatTimestampMillis(selectedMessage.timestamp)
              : 'Not available'
          }}</span>
        </div>
        <div>
          <span class="text-body-2 text-medium-emphasis">Payload</span>
          <json-viewer
            v-if="selectedMessage.parsedContent"
            :json="selectedMessage.content"
            class="border-thin rounded-lg"
          />
          <div class="raw-schema-snippet pa-3 bg-grey-lighten-5" v-else>
            {{ selectedMessage.content }}
          </div>
        </div>
      </v-card-text>
    </v-card>
  </v-dialog>
</template>

<style scoped lang="scss">
  @use 'vuetify/settings';

  .raw-schema-snippet {
    line-height: 1.2;
    font-size: 0.9rem;
    max-height: 500px;
    overflow: auto;
    cursor: text;
    border: 1px rgba(var(--v-border-color), var(--v-border-opacity)) solid;
    border-radius: settings.$border-radius-root;
  }
</style>
