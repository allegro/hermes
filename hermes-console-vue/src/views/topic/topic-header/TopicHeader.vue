<script setup lang="ts">
  import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
  import type { Owner } from '@/api/owner';
  import type { TopicWithSchema } from '@/api/topic';

  const props = defineProps<{
    topic: TopicWithSchema;
    owner: Owner;
  }>();

  const configStore = useAppConfigStore();
</script>

<template>
  <v-card density="compact">
    <v-card-item>
      <p class="text-overline">{{ $t('topicView.header.topic') }}</p>
      <div class="d-flex justify-space-between">
        <p class="text-h4 font-weight-bold">{{ props.topic.name }}</p>
        <v-tooltip :text="$t('topicView.header.actions.copyName')">
          <template v-slot:activator="{ props }">
            <v-btn icon="mdi-content-copy" variant="plain" v-bind="props" />
          </template>
        </v-tooltip>
      </div>
    </v-card-item>

    <v-card-text>
      <p class="text-subtitle-2">{{ props.topic.description }}</p>
    </v-card-text>

    <v-divider class="mx-4 mb-1" />

    <v-card-actions class="d-flex justify-space-between">
      <div>
        <v-btn
          class="text-none"
          prepend-icon="mdi-account-supervisor"
          :href="props.owner.url"
          target="_blank"
          color="blue"
        >
          {{ $t('topicView.header.owner') }} {{ props.owner.name }}
        </v-btn>
      </div>
      <div>
        <v-btn
          :disabled="configStore.appConfig.topic.readOnlyModeEnabled"
          prepend-icon="mdi-pencil"
          >{{ $t('topicView.header.actions.edit') }}
        </v-btn>
        <v-btn prepend-icon="mdi-content-copy"
          >{{ $t('topicView.header.actions.clone') }}
        </v-btn>
        <v-btn
          v-if="
            configStore.appConfig.topic.offlineRetransmissionEnabled &&
            topic.offlineStorage.enabled
          "
          prepend-icon="mdi-transmission-tower"
          >{{ $t('topicView.header.actions.offlineRetransmission') }}
        </v-btn>
        <v-btn color="red" prepend-icon="mdi-delete"
          >{{ $t('topicView.header.actions.remove') }}
        </v-btn>
      </div>
    </v-card-actions>
  </v-card>
</template>

<style scoped lang="scss"></style>
