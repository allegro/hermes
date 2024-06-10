<script setup lang="ts">
  import { computed, ref } from 'vue';
  import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
  import { useCreateTopic } from '@/composables/topic/use-create-topic/useCreateTopic';
  import { useEditTopic } from '@/composables/topic/use-edit-topic/useEditTopic';
  import { useGlobalI18n } from '@/i18n';
  import { useImportTopic } from '@/composables/topic/use-import-topic/useImportTopic';
  import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';
  import AceEditor from '@/components/ace-editor/AceEditor.vue';
  import ConsoleAlert from '@/components/console-alert/ConsoleAlert.vue';
  import SelectField from '@/components/select-field/SelectField.vue';
  import TextField from '@/components/text-field/TextField.vue';
  import type { TopicWithSchema } from '@/api/Topic';

  const props = defineProps<{
    topic: TopicWithSchema | null;
    group: string | null;
    operation: 'add' | 'edit';
  }>();
  const emit = defineEmits<{
    created: [topic: string];
    updated: [topic: string];
    cancel: [];
  }>();
  const configStore = useAppConfigStore();
  const notificationStore = useNotificationsStore();
  const { t } = useGlobalI18n();
  const {
    form,
    validators,
    dataSources,
    creatingOrUpdatingTopic,
    createOrUpdateTopic,
  } =
    props.operation === 'add'
      ? useCreateTopic(props.group!!)
      : useEditTopic(props.topic!!);
  const { importFormData } = useImportTopic();

  const ownerSelectorPlaceholder = computed(
    () =>
      configStore.loadedConfig.owner.sources.find(
        (source) => source.name === form.value.ownerSource?.name,
      )?.placeholder ?? '',
  );

  const isFormValid = ref(false);

  const importedFile = ref(null);

  function importForm() {
    importFormData(importedFile, form, dataSources);
  }

  const isAuthorizationSelected = computed(() => form.value.auth.enabled);

  const isAvroContentTypeSelected = computed(
    () => form.value.contentType === 'AVRO',
  );

  const isStoreOfflineSelected = computed(
    () => form.value.offlineStorage.enabled,
  );

  const isStoreOfflineInfiniteSelected = computed(
    () => form.value.offlineStorage.retentionTime.infinite,
  );

  const showTrackingAlert = computed(() => form.value.trackingEnabled);

  const showAvroAlert = computed(() => form.value.contentType === 'AVRO');

  async function submit() {
    if (isFormValid.value) {
      const isOperationSucceeded = await createOrUpdateTopic();
      if (isOperationSucceeded) {
        emit('created', form.value.name);
      }
    } else {
      notificationStore.dispatchNotification({
        title: t('notifications.form.validationError'),
        text: '',
        type: 'error',
      });
    }
  }
</script>

<template>
  <v-file-input
    v-if="operation === 'add'"
    :label="$t('topicForm.actions.import')"
    variant="outlined"
    accept=".json"
    v-model="importedFile"
    @change="importForm"
  ></v-file-input>
  <v-form
    v-model="isFormValid"
    @submit.prevent="submit()"
    class="d-flex flex-column row-gap-2"
  >
    <text-field
      v-if="operation === 'add'"
      v-model="form.name"
      :rules="validators.name"
      :label="$t('topicForm.fields.name.label')"
      :placeholder="$t('topicForm.fields.name.placeholder')"
      :autofocus="true"
    />

    <text-field
      v-model="form.description"
      :rules="validators.description"
      :label="$t('topicForm.fields.description.label')"
      :placeholder="$t('topicForm.fields.description.placeholder')"
    />

    <div class="d-flex flex-row column-gap-2">
      <select-field
        v-model="form.ownerSource"
        :rules="validators.ownerSource"
        :label="$t('topicForm.fields.ownerSource.label')"
        :items="dataSources.ownerSources.value"
        class="w-33"
      />

      <v-autocomplete
        v-if="form.ownerSource?.autocomplete"
        v-model="form.owner"
        v-model:search="form.ownerSearch"
        :loading="dataSources.loadingOwners.value"
        :rules="validators.owner"
        :label="$t('topicForm.fields.owner.label')"
        :items="dataSources.owners.value"
        density="comfortable"
        class="w-66"
        variant="outlined"
        :placeholder="ownerSelectorPlaceholder"
        persistent-placeholder
      />

      <text-field
        v-else
        v-model="form.owner"
        :rules="validators.owner"
        :label="$t('topicForm.fields.owner.label')"
        :placeholder="ownerSelectorPlaceholder"
        class="w-66"
      />
    </div>

    <v-divider />

    <v-switch
      v-model="form.auth.enabled"
      :label="$t('topicForm.fields.auth.enabled')"
      inset
      color="success"
      density="comfortable"
      hide-details
    />

    <text-field
      v-if="isAuthorizationSelected"
      v-model="form.auth.publishers"
      :label="$t('topicForm.fields.auth.publishers.label')"
      :placeholder="$t('topicForm.fields.auth.publishers.placeholder')"
    />

    <v-switch
      v-model="form.auth.unauthenticatedAccessEnabled"
      :label="$t('topicForm.fields.auth.unauthenticatedAccessEnabled')"
      inset
      color="success"
      density="comfortable"
      hide-details
    />

    <v-switch
      v-model="form.subscribingRestricted"
      :label="$t('topicForm.fields.restrictSubscribing')"
      inset
      color="success"
      density="comfortable"
      hide-details
    />

    <v-switch
      v-model="form.trackingEnabled"
      :label="$t('topicForm.fields.trackingEnabled')"
      inset
      color="success"
      density="comfortable"
      hide-details
    />

    <console-alert
      v-if="showTrackingAlert"
      :title="$t('topicForm.warnings.trackingEnabled.title')"
      :text="$t('topicForm.warnings.trackingEnabled.text')"
      type="warning"
      class="mb-4"
    />

    <v-divider />

    <select-field
      v-model="form.retentionTime.retentionUnit"
      :label="$t('topicForm.fields.retentionTime.unit')"
      :items="dataSources.retentionUnits"
    />

    <text-field
      v-model.number="form.retentionTime.duration"
      :rules="validators.retentionTimeDuration"
      type="number"
      :label="$t('topicForm.fields.retentionTime.duration')"
    />

    <console-alert
      :title="$t('topicForm.fields.ackHelpTitle')"
      :text="$t('topicForm.fields.ackHelpText')"
      type="info"
      class="mb-4"
      :link="$t('topicForm.fields.ackHelpLink')"
      :link-description="$t('topicForm.fields.ackHelpLinkDescription')"
    >
    </console-alert>

    <select-field
      v-model="form.ack"
      :rules="validators.ack"
      :label="$t('topicForm.fields.ack')"
      :items="dataSources.ackModes"
    />

    <select-field
      v-model="form.contentType"
      :label="$t('topicForm.fields.contentType')"
      :items="dataSources.contentTypes"
    />

    <text-field
      v-model.number="form.maxMessageSize"
      :rules="validators.maxMessageSize"
      type="number"
      :label="$t('topicForm.fields.maxMessageSize.label')"
      :suffix="$t('topicForm.fields.maxMessageSize.suffix')"
    />

    <v-divider />

    <v-switch
      v-model="form.offlineStorage.enabled"
      :label="$t('topicForm.fields.storeOffline')"
      inset
      color="success"
      density="comfortable"
      hide-details
    />

    <v-switch
      v-if="isStoreOfflineSelected"
      v-model="form.offlineStorage.retentionTime.infinite"
      :label="$t('topicForm.fields.retentionTime.infinite')"
      inset
      color="success"
      density="comfortable"
      hide-details
    />

    <text-field
      v-if="isStoreOfflineSelected && !isStoreOfflineInfiniteSelected"
      v-model.number="form.offlineStorage.retentionTime.duration"
      :rules="validators.offlineRetentionTime"
      type="number"
      :label="$t('topicForm.fields.retentionTime.duration')"
      :suffix="$t('topicForm.fields.retentionTime.days')"
    />

    <v-divider />

    <!--      text not in i18n because of a problem with escaping characters-->
    <console-alert
      style="white-space: pre"
      v-if="showAvroAlert"
      :title="$t('topicForm.info.avro.title')"
      text='{
    "name": "__metadata", "default": null,
    "type": ["null", {"type": "map", "values": "string"}],
    "doc": "Field used in Hermes internals to propagate metadata"
}'
      type="info"
      class="mb-4"
    />

    <AceEditor
      v-if="isAvroContentTypeSelected"
      v-model:model-value="form.schema"
      :label="t('topicForm.fields.schema')"
    />

    <div class="d-flex justify-end column-gap-2 mt-4">
      <v-btn
        variant="outlined"
        color="primary"
        :disabled="creatingOrUpdatingTopic"
        @click="$emit('cancel')"
        >{{ $t('topicForm.actions.cancel') }}
      </v-btn>
      <v-btn
        type="submit"
        variant="flat"
        color="primary"
        :loading="creatingOrUpdatingTopic"
        >{{
          props.operation === 'add'
            ? $t('topicForm.actions.create')
            : $t('topicForm.actions.update')
        }}
      </v-btn>
    </div>
  </v-form>
</template>

<style scoped lang="scss"></style>
