<script setup lang="ts">
  import { Ref, ref } from 'vue';
  import { subscriptionFqn } from '@/utils/subscription-utils/subscription-utils';
  import type { Constraint } from '@/api/constraints';

  const props = defineProps<{
    isSubscription: boolean;
  }>();

  const consumersNumber: Ref<number> = ref(1);
  const reason: Ref<string> = ref('');
  const topicName: Ref<string> = ref('');
  const subscriptionName: Ref<string> = ref('');

  const emit = defineEmits<{
    create: [resourceId: string, constraint: Constraint];
    cancel: [];
  }>();

  const onCreated = async () => {
    const constraint: Constraint = {
      consumersNumber: consumersNumber.value,
      reason: reason.value,
    };
    let resourceId = '';
    if (props.isSubscription) {
      resourceId = subscriptionFqn(topicName.value, subscriptionName.value);
    } else {
      resourceId = topicName.value;
    }
    emit('create', resourceId, constraint);
  };
</script>

<template>
  <v-form @submit.prevent>
    <v-card>
      <v-card-title>
        {{
          props.isSubscription
            ? $t('constraints.createForm.createSubscriptionTitle')
            : $t('constraints.createForm.createTopicTitle')
        }}
      </v-card-title>
      <v-card-item>
        <v-text-field
          :label="$t('constraints.createForm.topicName')"
          type="text"
          v-model="topicName"
          data-testid="createConstraintTopicNameInput"
        >
        </v-text-field>
      </v-card-item>
      <v-card-item v-if="props.isSubscription">
        <v-text-field
          :label="$t('constraints.createForm.subscriptionName')"
          type="text"
          v-model="subscriptionName"
          data-testid="createConstraintSubscriptionNameInput"
        >
        </v-text-field>
      </v-card-item>
      <v-card-item>
        <v-text-field
          :label="$t('constraints.createForm.consumersNumber')"
          type="number"
          v-model="consumersNumber"
          data-testid="createConstraintConsumersNumberInput"
        >
        </v-text-field>
      </v-card-item>
      <v-card-item>
        <v-text-field
          :label="$t('constraints.createForm.reason')"
          type="text"
          v-model="reason"
          data-testid="createConstraintReasonInput"
        >
        </v-text-field>
      </v-card-item>
      <v-card-item class="text-right">
        <v-btn
          color="primary"
          class="mr-4"
          @click="onCreated"
          data-testid="createConstraintSave"
        >
          {{ $t('constraints.createForm.save') }}
        </v-btn>
        <v-btn
          color="orange"
          @click="emit('cancel')"
          data-testid="createConstraintCancel"
        >
          {{ $t('constraints.createForm.cancel') }}
        </v-btn>
      </v-card-item>
    </v-card>
  </v-form>
</template>
