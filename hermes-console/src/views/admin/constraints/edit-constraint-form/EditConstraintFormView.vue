<script setup lang="ts">
  import { Ref, ref } from 'vue';
  import { useI18n } from 'vue-i18n';
  import type { Constraint } from '@/api/constraints';

  const { t } = useI18n();

  const props = defineProps<{
    resourceId: string;
    constraint: Constraint;
  }>();

  const emit = defineEmits<{
    update: [resourceId: string, constraint: Constraint];
    delete: [resourceId: string];
    cancel: [];
  }>();

  const consumersNumber: Ref<number> = ref(props.constraint.consumersNumber);
  const reason: Ref<number> = ref(props.constraint.reason);

  const onUpdated = () => {
    const constraint: Constraint = {
      consumersNumber: consumersNumber.value,
      reason: reason.value,
    };
    emit('update', props.resourceId, constraint);
  };

  const onDeleted = () => {
    emit('delete', props.resourceId);
  };
</script>

<template>
  <v-form @submit.prevent>
    <v-card>
      <v-card-title class="text-wrap">
        {{ t('constraints.editForm.title', { resourceId }) }}
      </v-card-title>
      <v-card-item>
        <v-text-field
          :label="$t('constraints.editForm.consumersNumber')"
          type="number"
          v-model="consumersNumber"
          data-testid="editConstraintConsumersNumberInput"
        >
        </v-text-field>
      </v-card-item>
      <v-card-item>
        <v-text-field
          :label="$t('constraints.editForm.reason')"
          type="text"
          v-model="reason"
          data-testid="editConstraintReasonInput"
        >
        </v-text-field>
      </v-card-item>
      <v-card-item class="text-right">
        <v-btn
          color="primary"
          class="mr-4"
          @click="onUpdated"
          data-testid="editConstraintSave"
        >
          {{ $t('constraints.editForm.save') }}
        </v-btn>
        <v-btn
          color="red"
          class="mr-4"
          @click="onDeleted"
          data-testid="editConstraintRemove"
        >
          {{ $t('constraints.editForm.remove') }}
        </v-btn>
        <v-btn
          color="orange"
          @click="$emit('cancel')"
          data-testid="editConstraintCancel"
        >
          {{ $t('constraints.editForm.cancel') }}
        </v-btn>
      </v-card-item>
    </v-card>
  </v-form>
</template>
