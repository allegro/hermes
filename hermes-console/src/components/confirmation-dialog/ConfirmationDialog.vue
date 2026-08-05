<script setup lang="ts">
  import { ref, watch } from 'vue';
  import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';

  const props = withDefaults(
    defineProps<{
      actionButtonEnabled: boolean;
      title: string;
      text: string;
      actionColor?: string;
      icon?: string;
      actionText?: string;
    }>(),
    {
      actionColor: 'error',
      icon: 'mdi-alert',
    },
  );

  const configStore = useAppConfigStore();
  const model = defineModel<boolean>({ default: false });
  const emit = defineEmits<{
    action: [];
    cancel: [];
  }>();

  const confirmationText = ref<string>();

  watch(model, () => {
    confirmationText.value = undefined;
  });

  function onDialogModelUpdate(value: boolean) {
    if (!value && model.value) {
      confirmationText.value = undefined;
      emit('cancel');
    }
    model.value = value;
  }

  function confirm() {
    confirmationText.value = undefined;
    emit('action');
  }

  function cancel() {
    confirmationText.value = undefined;
    emit('cancel');
  }
</script>

<template>
  <v-dialog
    :model-value="model"
    width="100%"
    min-width="30%"
    :persistent="!actionButtonEnabled"
    @update:model-value="onDialogModelUpdate"
  >
    <v-form @submit.prevent>
      <v-card>
        <v-card-item class="border-b">
          <v-card-title v-if="title" class="text-wrap">
            <v-avatar variant="tonal" :color="props.actionColor" start>
              <v-icon :color="props.actionColor" size="24">
                {{ props.icon }}
              </v-icon>
            </v-avatar>
            {{ props.title }}
          </v-card-title>
        </v-card-item>

        <v-card-text class="pt-4">
          <span class="text-body-1">{{ props.text }}</span>
        </v-card-text>
        <v-card-text
          v-if="configStore.loadedConfig?.console.criticalEnvironment"
        >
          <v-text-field
            :label="$t('confirmationDialog.confirmText')"
            type="input"
            v-model="confirmationText"
            prepend-inner-icon="mdi-alert"
          />
        </v-card-text>

        <v-card-actions>
          <v-col class="d-flex column-gap-2 justify-end">
            <v-btn
              variant="flat"
              :color="props.actionColor"
              @click="confirm"
              :disabled="
                (configStore.loadedConfig?.console.criticalEnvironment &&
                  confirmationText !== 'prod') ||
                !actionButtonEnabled
              "
            >
              {{ props.actionText ?? $t('confirmationDialog.confirm') }}
            </v-btn>
            <v-btn
              variant="flat"
              :disabled="!actionButtonEnabled"
              @click="cancel"
            >
              {{ $t('confirmationDialog.cancel') }}
            </v-btn>
          </v-col>
        </v-card-actions>
      </v-card>
    </v-form>
  </v-dialog>
</template>

<style scoped></style>
