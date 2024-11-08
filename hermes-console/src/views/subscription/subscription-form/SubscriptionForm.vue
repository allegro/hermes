<script setup lang="ts">
  import { computed, ref } from 'vue';
  import { isAdmin } from '@/utils/roles-util';
  import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
  import { useCreateSubscription } from '@/composables/subscription/use-create-subscription/useCreateSubscription';
  import { useEditSubscription } from '@/composables/subscription/use-edit-subscription/useEditSubscription';
  import { useGlobalI18n } from '@/i18n';
  import { useImportSubscription } from '@/composables/subscription/use-import-subscription/useImportSubscription';
  import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';
  import ConsoleAlert from '@/components/console-alert/ConsoleAlert.vue';
  import SelectField from '@/components/select-field/SelectField.vue';
  import SubscriptionHeaderFilters from '@/views/subscription/subscription-form/subscription-header-filters/SubscriptionHeaderFilters.vue';
  import SubscriptionPathFilters from '@/views/subscription/subscription-form/subscription-basic-filters/SubscriptionPathFilters.vue';
  import SubscriptionPathFiltersDebug from '@/views/subscription/subscription-form/subscription-basic-filters/SubscriptionPathFiltersDebug.vue';
  import TextField from '@/components/text-field/TextField.vue';
  import TooltipIcon from '@/components/tooltip-icon/TooltipIcon.vue';
  import type { Role } from '@/api/role';
  import type { Subscription } from '@/api/subscription';

  const props = defineProps<{
    topic: string;
    subscription: Subscription | null;
    operation: 'add' | 'edit';
    roles: Role[] | undefined;
    paths: string[];
  }>();
  const emit = defineEmits<{
    created: [subscription: string];
    updated: [subscription: string];
    cancel: [];
  }>();
  const configStore = useAppConfigStore();
  const notificationStore = useNotificationsStore();
  const { t } = useGlobalI18n();
  const {
    form,
    validators,
    dataSources,
    creatingOrUpdatingSubscription,
    createOrUpdateSubscription,
  } =
    props.operation === 'add'
      ? useCreateSubscription(props.topic)
      : useEditSubscription(props.topic, props.subscription!!);
  const { importFormData } = useImportSubscription();
  const showHighRequestTimeoutAlert = computed(
    () =>
      form.value.subscriptionPolicy.requestTimeout >=
        configStore.loadedConfig.subscription.requestTimeoutWarningThreshold &&
      form.value.deliveryType === 'SERIAL',
  );
  const isSerialDeliveryTypeSelected = computed(
    () => form.value.deliveryType === 'SERIAL',
  );
  const isBatchDeliveryTypeSelected = computed(
    () => form.value.deliveryType === 'BATCH',
  );
  const ownerSelectorPlaceholder = computed(
    () =>
      configStore.loadedConfig.owner.sources.find(
        (source) => source.name === form.value.ownerSource?.name,
      )?.placeholder ?? '',
  );
  const showTrackingModeAlert = computed(
    () => form.value.messageDeliveryTrackingMode === 'trackingAll',
  );
  const isFormValid = ref(false);

  const importedFile = ref(null);

  function importForm() {
    importFormData(importedFile, form, dataSources);
  }

  async function submit() {
    if (isFormValid.value || isAdmin(props.roles)) {
      const isOperationSucceeded = await createOrUpdateSubscription();
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
  <console-alert
    v-if="isAdmin(roles)"
    :title="$t('subscriptionForm.warnings.adminForm.title')"
    :text="$t('subscriptionForm.warnings.adminForm.text')"
    type="warning"
    class="mb-4"
  />
  <v-file-input
    v-if="operation === 'add'"
    :label="$t('subscriptionForm.actions.import')"
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
      :label="$t('subscriptionForm.fields.name.label')"
      :placeholder="$t('subscriptionForm.fields.name.placeholder')"
      :autofocus="true"
    />

    <text-field
      v-model="form.endpoint"
      :rules="validators.endpoint"
      :label="$t('subscriptionForm.fields.endpoint.label')"
      :placeholder="$t('subscriptionForm.fields.endpoint.placeholder')"
    />

    <text-field
      v-model="form.description"
      :rules="validators.description"
      :label="$t('subscriptionForm.fields.description.label')"
      :placeholder="$t('subscriptionForm.fields.description.placeholder')"
    />

    <div class="d-flex flex-row column-gap-2">
      <select-field
        v-model="form.ownerSource"
        :rules="validators.ownerSource"
        :label="$t('subscriptionForm.fields.ownerSource.label')"
        :items="dataSources.ownerSources.value"
        class="w-33"
      />

      <v-autocomplete
        v-if="form.ownerSource?.autocomplete"
        v-model="form.owner"
        v-model:search="form.ownerSearch"
        :loading="dataSources.loadingOwners.value"
        :rules="validators.owner"
        :label="$t('subscriptionForm.fields.owner.label')"
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
        :label="$t('subscriptionForm.fields.owner.label')"
        :placeholder="ownerSelectorPlaceholder"
        class="w-66"
      />
    </div>

    <select-field
      v-model="form.deliveryType"
      :rules="validators.deliveryType"
      :label="$t('subscriptionForm.fields.deliveryType.label')"
      :items="dataSources.deliveryTypes"
    />

    <select-field
      v-model="form.contentType"
      :rules="validators.contentType"
      :label="$t('subscriptionForm.fields.contentType.label')"
      :items="dataSources.contentTypes.value"
    />

    <select-field
      v-model="form.mode"
      :rules="validators.mode"
      :label="$t('subscriptionForm.fields.mode.label')"
      :items="dataSources.deliveryModes"
    />

    <text-field
      v-if="isSerialDeliveryTypeSelected"
      v-model="form.subscriptionPolicy.rateLimit"
      :rules="validators.rateLimit"
      type="number"
      :label="$t('subscriptionForm.fields.rateLimit.label')"
      :placeholder="$t('subscriptionForm.fields.rateLimit.placeholder')"
      :suffix="$t('subscriptionForm.fields.rateLimit.suffix')"
    />

    <div v-if="isBatchDeliveryTypeSelected">
      <text-field
        v-model="form.subscriptionPolicy.batchSize"
        :rules="validators.batchSize"
        type="number"
        :label="$t('subscriptionForm.fields.batchSize.label')"
        :placeholder="$t('subscriptionForm.fields.batchSize.placeholder')"
        :suffix="$t('subscriptionForm.fields.batchSize.suffix')"
      />

      <text-field
        v-model="form.subscriptionPolicy.batchTime"
        :rules="validators.batchTime"
        type="number"
        :label="$t('subscriptionForm.fields.batchTime.label')"
        :placeholder="$t('subscriptionForm.fields.batchTime.placeholder')"
        :suffix="$t('subscriptionForm.fields.batchTime.suffix')"
      />

      <text-field
        v-model="form.subscriptionPolicy.batchVolume"
        :rules="validators.batchVolume"
        type="number"
        :label="$t('subscriptionForm.fields.batchVolume.label')"
        :placeholder="$t('subscriptionForm.fields.batchVolume.placeholder')"
        :suffix="$t('subscriptionForm.fields.batchVolume.suffix')"
      />
    </div>

    <text-field
      v-model="form.subscriptionPolicy.requestTimeout"
      :rules="validators.requestTimeout"
      type="number"
      :label="$t('subscriptionForm.fields.requestTimeout.label')"
      :placeholder="$t('subscriptionForm.fields.requestTimeout.placeholder')"
      :suffix="$t('subscriptionForm.fields.requestTimeout.suffix')"
    />

    <console-alert
      v-if="showHighRequestTimeoutAlert"
      :title="$t('subscriptionForm.warnings.highRequestTimeout.title')"
      :text="$t('subscriptionForm.warnings.highRequestTimeout.text')"
      type="warning"
      class="mb-4"
    />

    <text-field
      v-if="isSerialDeliveryTypeSelected"
      v-model.number="form.subscriptionPolicy.sendingDelay"
      :rules="validators.sendingDelay"
      type="number"
      :label="$t('subscriptionForm.fields.sendingDelay.label')"
      :placeholder="$t('subscriptionForm.fields.sendingDelay.placeholder')"
      :suffix="$t('subscriptionForm.fields.sendingDelay.suffix')"
    />

    <text-field
      v-model="form.subscriptionPolicy.inflightMessageTTL"
      :rules="validators.inflightMessageTTL"
      type="number"
      :label="$t('subscriptionForm.fields.inflightMessageTTL.label')"
      :placeholder="
        $t('subscriptionForm.fields.inflightMessageTTL.placeholder')
      "
      :suffix="$t('subscriptionForm.fields.inflightMessageTTL.suffix')"
    />

    <text-field
      v-if="isAdmin(roles)"
      v-model="form.subscriptionPolicy.inflightMessagesCount"
      :rules="validators.inflightMessagesCount"
      prepend-icon="$warning"
      type="number"
      :label="$t('subscriptionForm.fields.inflightMessagesCount.label')"
      :placeholder="
        $t('subscriptionForm.fields.inflightMessagesCount.placeholder')
      "
    />

    <v-divider />

    <v-switch
      v-model="form.retryOn4xx"
      :label="$t('subscriptionForm.fields.retryOn4xx.label')"
      inset
      color="success"
      density="comfortable"
      hide-details
      class="mt-1"
    />

    <text-field
      v-model="form.subscriptionPolicy.retryBackoff"
      :rules="validators.retryBackoff"
      type="number"
      :label="$t('subscriptionForm.fields.retryBackoff.label')"
      :placeholder="$t('subscriptionForm.fields.retryBackoff.placeholder')"
      :suffix="$t('subscriptionForm.fields.retryBackoff.suffix')"
      class="mt-3"
    />

    <text-field
      v-if="isSerialDeliveryTypeSelected"
      v-model="form.subscriptionPolicy.retryBackoffMultiplier"
      :rules="validators.retryBackoffMultiplier"
      type="number"
      :label="$t('subscriptionForm.fields.retryBackoffMultiplier.label')"
      :placeholder="
        $t('subscriptionForm.fields.retryBackoffMultiplier.placeholder')
      "
    />

    <text-field
      v-if="isSerialDeliveryTypeSelected"
      v-model="form.subscriptionPolicy.backoffMaxIntervalInSec"
      :rules="validators.backoffMaxIntervalInSec"
      type="number"
      :suffix="$t('subscriptionForm.fields.backoffMaxIntervalInSec.suffix')"
      :label="$t('subscriptionForm.fields.backoffMaxIntervalInSec.label')"
      :placeholder="
        $t('subscriptionForm.fields.backoffMaxIntervalInSec.placeholder')
      "
    />

    <v-divider />

    <select-field
      v-model="form.messageDeliveryTrackingMode"
      :rules="validators.messageDeliveryTrackingMode"
      :label="$t('subscriptionForm.fields.messageDeliveryTrackingMode.label')"
      :items="dataSources.messageDeliveryTrackingModes"
      class="mt-5"
    />

    <console-alert
      v-if="showTrackingModeAlert"
      :title="$t('subscriptionForm.warnings.trackingMode.title')"
      :text="$t('subscriptionForm.warnings.trackingMode.text')"
      type="warning"
      class="mb-4"
    />

    <v-divider />
    <v-row>
      <v-col>
        <span class="text-subtitle-1 mb-2">{{
          $t('subscriptionForm.sections.filters.heading')
        }}</span>
      </v-col>
      <v-col class="text-right">
        <subscription-path-filters-debug
          :topic="props.topic"
          :paths="paths"
          v-model="form.pathFilters"
          :edit-enabled="true"
        />
      </v-col>
    </v-row>
    <subscription-path-filters :paths="paths" v-model="form.pathFilters" />

    <v-divider class="mb-4" />

    <subscription-header-filters v-model="form.headerFilters" />

    <v-divider class="mb-4" />

    <select-field
      v-model="form.monitoringDetails.severity"
      :rules="validators.monitoringSeverity"
      :label="$t('subscriptionForm.fields.monitoringSeverity.label')"
      :items="dataSources.monitoringSeverities"
    />

    <text-field
      v-model="form.monitoringDetails.reaction"
      :label="$t('subscriptionForm.fields.monitoringReaction.label')"
      :placeholder="
        $t('subscriptionForm.fields.monitoringReaction.placeholder')
      "
    />

    <v-switch
      v-model="form.deliverUsingHttp2"
      :label="$t('subscriptionForm.fields.deliverUsingHttp2.label')"
      inset
      color="success"
      density="comfortable"
      hide-details
    />

    <v-switch
      v-model="form.attachSubscriptionIdentityHeaders"
      :label="
        $t('subscriptionForm.fields.attachSubscriptionIdentityHeaders.label')
      "
      inset
      color="success"
      density="comfortable"
      hide-details
    />

    <v-switch
      v-model="form.deleteSubscriptionAutomatically"
      :label="
        $t('subscriptionForm.fields.deleteSubscriptionAutomatically.label')
      "
      inset
      color="success"
      density="comfortable"
      hide-details
    />

    <div
      v-for="[propertyName, propertyValue] in Object.entries(
        configStore.appConfig!.subscription.endpointAddressResolverMetadata,
      )"
      :key="propertyName"
      class="d-flex flex-row"
    >
      <v-switch
        v-model="form.endpointAddressResolverMetadata[propertyName]"
        inset
        :label="propertyValue.title"
        v-if="propertyValue.type == 'boolean'"
        color="success"
        density="comfortable"
        hide-details
      />
      <tooltip-icon :content="propertyValue.hint" class="align-self-center" />
    </div>

    <div class="d-flex justify-end column-gap-2 mt-4">
      <v-btn
        variant="outlined"
        color="primary"
        :disabled="creatingOrUpdatingSubscription"
        @click="$emit('cancel')"
        >{{ $t('subscriptionForm.actions.cancel') }}
      </v-btn>
      <v-btn
        type="submit"
        variant="flat"
        color="primary"
        :loading="creatingOrUpdatingSubscription"
        >{{
          props.operation === 'add'
            ? $t('subscriptionForm.actions.create')
            : $t('subscriptionForm.actions.update')
        }}
      </v-btn>
    </div>
  </v-form>
</template>

<style scoped lang="scss"></style>
