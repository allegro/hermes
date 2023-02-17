<script setup lang="ts">
import type { TopicWithSchema } from "@/api/topic";
import type { Owner } from "@/api/owner";

interface Props {
  topic: TopicWithSchema;
  owner: Owner;
}

const { topic, owner } = defineProps<Props>();
</script>

<template>
  <v-card density="compact">
    <v-card-item>
      <p class="text-overline">{{ $t('topicView.header.topic') }}</p>
      <div class="d-flex justify-space-between">
        <p class="text-h4 font-weight-bold">{{ topic.name }}</p>
        <v-tooltip text="Copy event name">
          <template v-slot:activator="{ props }">
            <v-btn icon="mdi-content-copy" variant="plain" v-bind="props" />
          </template>
        </v-tooltip>
      </div>
      <p v-if="owner" class="text-subtitle-1">{{ $t('topicView.header.owner') }} {{ owner.name }}</p>
    </v-card-item>

    <v-card-text>
      <p class="text-subtitle-2">{{ topic.description }}</p>
    </v-card-text>

    <v-divider class="mx-4 mb-1"/>

    <v-card-actions class="d-flex justify-end">
      <v-btn prepend-icon="mdi-pencil">{{ $t('topicView.header.actions.edit') }}</v-btn>
      <v-btn prepend-icon="mdi-content-copy">{{ $t('topicView.header.actions.clone') }}</v-btn>
      <v-btn prepend-icon="mdi-transmission-tower">{{ $t('topicView.header.actions.offlineRetransmission') }}</v-btn>
      <v-btn color="red" prepend-icon="mdi-delete">{{ $t('topicView.header.actions.remove') }}</v-btn>
    </v-card-actions>
  </v-card>
</template>

<style scoped lang="scss"></style>
