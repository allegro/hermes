<script setup lang="ts">
  import { Constraint } from '@/api/constraints';
  import { ref } from 'vue';
  import CreateConstraintFormView from '@/views/admin/constraints/create-constraint-form/CreateConstraintFormView.vue';

  const showDialog = ref(false);

  const props = defineProps<{
    isSubscription: boolean;
  }>();

  const emit = defineEmits<{
    create: [resourceId: string, constraint: Constraint];
  }>();

  const onCreated = (resourceId: string, constraint: Constraint) => {
    emit('create', resourceId, constraint);
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
    <CreateConstraintFormView
      :isSubscription="props.isSubscription"
      @create="onCreated"
      @cancel="onCanceled"
    >
    </CreateConstraintFormView>
  </v-dialog>
</template>
