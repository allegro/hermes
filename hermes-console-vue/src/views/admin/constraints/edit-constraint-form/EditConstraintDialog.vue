<script setup lang="ts">
  import { Constraint } from '@/api/constraints';
  import { ref } from 'vue';
  import EditConstraintFormView from '@/views/admin/constraints/edit-constraint-form/EditConstraintFormView.vue';

  const showDialog = ref(false);

  const props = defineProps<{
    resourceId: string;
    constraint: Constraint;
  }>();

  const emit = defineEmits<{
    update: [resourceId: string, constraint: Constraint];
    delete: [resourceId: string];
  }>();

  const onUpdated = (resourceId: string, constraint: Constraint) => {
    emit('update', resourceId, constraint);
    showDialog.value = false;
  };

  const onDeleted = (resourceId: string) => {
    emit('delete', resourceId);
    showDialog.value = false;
  };

  const onCanceled = () => {
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
    <EditConstraintFormView
      :resource-id="props.resourceId"
      :constraint="props.constraint"
      @update="onUpdated"
      @delete="onDeleted"
      @cancel="onCanceled"
    >
    </EditConstraintFormView>
  </v-dialog>
</template>
