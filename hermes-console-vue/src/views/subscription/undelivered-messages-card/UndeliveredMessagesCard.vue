<script setup lang="ts">
  import { formatTimestamp } from '@/utils/date-formatter/date-formatter';
  import type { SentMessageTrace } from '@/api/subscription-undelivered';

  interface UndeliveredMessagesCardProps {
    undeliveredMessages: SentMessageTrace[];
  }

  const props = defineProps<UndeliveredMessagesCardProps>();
</script>

<template>
  <v-card class="mb-2">
    <template #title>
      <p class="font-weight-bold">Last 100 undelivered messages</p>
    </template>
    <v-table density="compact">
      <thead>
        <tr>
          <th class="text-left">#</th>
          <th class="text-left">MessageId</th>
          <th class="text-left">Status</th>
          <th class="text-left">Reason</th>
          <th class="text-left">Timestamp</th>
          <th class="text-left"></th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="(message, index) in props.undeliveredMessages.slice(0, 100)"
          :key="index"
        >
          <td>{{ index + 1 }}</td>
          <td>{{ message.messageId }}</td>
          <td>{{ message.status }}</td>
          <td>{{ message.reason }}</td>
          <td>{{ formatTimestamp(message.timestamp) }}</td>
          <td>
            <v-btn
              density="compact"
              icon="mdi-magnify"
              size="small"
              variant="flat"
            />
          </td>
        </tr>
      </tbody>
    </v-table>
  </v-card>
</template>

<style scoped lang="scss"></style>
