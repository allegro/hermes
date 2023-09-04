<script setup lang="ts">
  import { ref } from 'vue';
  import OfflineRetransmissionForm from '@/views/topic/offline-retransmission/OfflineRetransmissionForm.vue';

  const showDialog = ref(false);

  const emit = defineEmits<{
    retransmit: [targetTopic: string, from: string, to: string];
  }>();

  const onRetransmit = (targetTopic: string, from: string, to: string) => {
    emit('retransmit', targetTopic, from, to);
    showDialog.value = false;
  };

  const onCancel = () => {
    showDialog.value = false;
  };
</script>

<template>
  <v-dialog
    activator="parent"
    width="100%"
    min-width="50%"
    v-model="showDialog"
  >
    <OfflineRetransmissionForm @retransmit="onRetransmit" @cancel="onCancel">
    </OfflineRetransmissionForm>
  </v-dialog>
</template>
