<script lang="ts" setup>
  import '@/components/ace-editor/ace-config';
  import { defineProps } from 'vue';
  import { useGlobalI18n } from '@/i18n';
  import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';
  import { useTheme } from 'vuetify';
  import { VAceEditor } from 'vue3-ace-editor';

  const theme = useTheme();
  const notificationStore = useNotificationsStore();

  const { t } = useGlobalI18n();
  const props = defineProps<{
    modelValue: string;
    label?: string;
    placeholder?: string;
  }>();
  const emit = defineEmits(['update:modelValue']);
  const beautify = () => {
    try {
      const obj_message = JSON.parse(props.modelValue || '');
      emit('update:modelValue', JSON.stringify(obj_message, null, 4));
    } catch (e) {
      notificationStore.dispatchNotification({
        title: '',
        text: t('notifications.form.beautifyError'),
        type: 'error',
      });
    }
  };
</script>

<template>
  <div style="border: 1px solid #777777; padding: 10px">
    <p v-if="label" class="v-label">{{ props.label }}</p>
    <v-ace-editor
      :options="{ useWorker: true }"
      :placeholder="props.placeholder"
      :theme="theme.global.name.value === 'light' ? 'github' : 'monokai'"
      :value="modelValue"
      class="my-3"
      lang="json"
      style="height: 300px"
      @update:value="$emit('update:modelValue', $event)"
    />
    <v-btn
      color="primary"
      variant="outlined"
      @click="beautify"
      class="text-capitalize"
    >
      {{ t('topicForm.fields.beautify') }}
    </v-btn>
  </div>
</template>

<style lang="scss">
  .ace_placeholder {
    color: #777;
    position: inherit;
    z-index: 1;
  }
</style>
