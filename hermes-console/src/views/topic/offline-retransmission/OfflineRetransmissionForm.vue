<script setup lang="ts">
  import { ref } from 'vue';

  const emit = defineEmits<{
    retransmit: [targetTopic: string, from: string, to: string];
    cancel: [];
  }>();

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
      <v-card-title> {{ $t('offlineRetransmission.title') }}</v-card-title>
      <v-card-subtitle>{{
        $t('offlineRetransmission.subtitle')
      }}</v-card-subtitle>
      <v-card-item>
        <v-text-field
          :label="$t('offlineRetransmission.targetTopic')"
          type="text"
          v-model="targetTopic"
          data-testid="offlineRetransmissionTargetTopicNameInput"
        >
        </v-text-field>
      </v-card-item>
      <v-card-item>
        <v-text-field
          :label="$t('offlineRetransmission.startTimestamp')"
          type="datetime-local"
          v-model="startTimestamp"
          data-testid="offlineRetransmissionStartTimestampInput"
        >
        </v-text-field>
      </v-card-item>
      <v-card-item>
        <v-text-field
          :label="$t('offlineRetransmission.endTimestamp')"
          type="datetime-local"
          v-model="endTimestamp"
          data-testid="offlineRetransmissionEndTimestampInput"
        >
        </v-text-field>
      </v-card-item>
      <v-card-item class="text-right">
        <v-btn
          color="primary"
          class="mr-4"
          @click="onRetransmit"
          data-testid="offlineRetransmissionRetransmit"
        >
          {{ $t('constraints.createForm.save') }}
        </v-btn>
        <v-btn
          color="orange"
          @click="emit('cancel')"
          data-testid="offlineRetransmissionCancel"
        >
          {{ $t('constraints.createForm.cancel') }}
        </v-btn>
      </v-card-item>
    </v-card>
  </v-form>
</template>
