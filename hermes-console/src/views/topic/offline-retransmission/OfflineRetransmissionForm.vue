<script setup lang="ts">
  import { ref } from 'vue';
  import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
  import SimpleLink from '@/components/simple-link/SimpleLink.vue';

  const emit = defineEmits<{
    retransmit: [targetTopic: string, from: string, to: string];
    cancel: [];
  }>();

  const configStore = useAppConfigStore();
  const targetTopic = ref('');
  const startTimestamp = ref(new Date().toISOString().slice(0, -5));
  const endTimestamp = ref(new Date().toISOString().slice(0, -5));

  const onRetransmit = () => {
    emit(
      'retransmit',
      targetTopic.value,
      `${startTimestamp.value}Z`,
      `${endTimestamp.value}Z`,
    );
  };
</script>

<template>
  <v-form @submit.prevent>
    <v-card>
      <v-card-item class="border-b">
        <div class="d-flex justify-space-between align-start">
          <div>
            <v-card-title>
              {{ $t('offlineRetransmission.title') }}
            </v-card-title>
            <div class="d-flex">
              <v-card-subtitle
                >{{ $t('offlineRetransmission.subtitle') }}
                <simple-link
                  :href="
                    configStore.loadedConfig.topic.offlineRetransmission
                      .fromViewDocsUrl
                  "
                  :text="
                    $t('offlineRetransmission.titleRetransmissionFromView')
                  "
                  open-in-new-tab
                />
              </v-card-subtitle>
            </div>
          </div>
        </div>
      </v-card-item>

      <v-card-item>
        <v-text-field
          variant="outlined"
          :label="$t('offlineRetransmission.targetTopic')"
          type="text"
          v-model="targetTopic"
          data-testid="offlineRetransmissionTargetTopicNameInput"
        />

        <v-text-field
          variant="outlined"
          :label="$t('offlineRetransmission.startTimestamp')"
          type="datetime-local"
          v-model="startTimestamp"
          data-testid="offlineRetransmissionStartTimestampInput"
        />

        <v-text-field
          variant="outlined"
          :label="$t('offlineRetransmission.endTimestamp')"
          type="datetime-local"
          v-model="endTimestamp"
          data-testid="offlineRetransmissionEndTimestampInput"
        />
      </v-card-item>

      <v-card-item class="text-right">
        <v-btn
          color="primary"
          class="mr-4 text-capitalize"
          @click="onRetransmit"
          data-testid="offlineRetransmissionRetransmit"
        >
          {{ $t('constraints.createForm.create') }}
        </v-btn>
        <v-btn
          @click="emit('cancel')"
          class="text-capitalize"
          data-testid="offlineRetransmissionCancel"
        >
          {{ $t('constraints.createForm.cancel') }}
        </v-btn>
      </v-card-item>
    </v-card>
  </v-form>
</template>
