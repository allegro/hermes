import { computed, ref, watch } from 'vue';
import {
  fetchOwnersSources,
  hermesClient,
  searchOwners,
} from '@/api/hermes-client';
import { matchRegex, max, min, required } from '@/utils/validators';
import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
import { useAppNotifications } from '@/store/app-notifications/useAppNotifications';
import type { CreateSubscriptionFormRequestBody } from '@/api/subscription';
import type {
  DataSources,
  FormValidators,
  SubscriptionForm,
  UseCreateSubscription,
  UseCreateSubscriptionErrors,
} from '@/composables/subscription/use-create-subscription/types';
import type { OwnerSource } from '@/api/owner';
import type { Ref } from 'vue';
import type { SelectFieldOption } from '@/components/select-field/types';

const formValidators = (form: Ref<SubscriptionForm>): FormValidators => {
  return {
    name: [required(), matchRegex(/^[a-zA-Z0-9.-]+$/, 'Invalid name')],
    endpoint: [required()],
    description: [required()],
    ownerSource: [required()],
    owner: [required()],
    contentType: [required()],
    deliveryType: [required()],
    mode: [required()],
    rateLimit: [required(), min(0), max(5000)],
    batchSize: [required(), min(1), max(1000000)],
    batchTime: [required(), min(1), max(1000000)],
    batchVolume: [required(), min(1), max(1000000)],
    requestTimeout: [
      required(),
      min(0),
      max(form.value.deliveryType === 'SERIAL' ? 10000 : 1000000),
    ],
    sendingDelay: [required(), min(0), max(5000)],
    inflightMessageTTL: [required(), min(0), max(7200)],
    retryBackoff: [required(), min(0), max(1000000)],
    retryBackoffMultiplier: [required(), min(1), max(10)],
    messageDeliveryTrackingMode: [required()],
    monitoringSeverity: [required()],
  };
};

export function useCreateSubscription(topic: string): UseCreateSubscription {
  const { loadedConfig } = useAppConfigStore();
  const errors = ref<UseCreateSubscriptionErrors>({
    fetchOwnerSources: null,
    fetchOwners: null,
  });
  const rawDataSources = useDataSources(errors);
  const form = ref<SubscriptionForm>({
    name: '',
    endpoint: '',
    description: '',
    ownerSource: null,
    owner: '',
    ownerSearch: '',
    contentType: '',
    deliveryType: loadedConfig.subscription.defaults.deliveryType,
    mode: rawDataSources.deliveryModes[0].value,
    subscriptionPolicy: {
      rateLimit: null,
      inflightMessageTTL:
        loadedConfig.subscription.defaults.subscriptionPolicy.messageTtl ||
        3600,
      retryBackoff: 1000,
      retryBackoffMultiplier: 1.0,
      sendingDelay: 0,
      requestTimeout:
        loadedConfig.subscription.defaults.subscriptionPolicy.requestTimeout ||
        1000,
      batchSize: null,
      batchTime: null,
      batchVolume: null,
    },
    retryOn4xx: false,
    messageDeliveryTrackingMode:
      rawDataSources.messageDeliveryTrackingModes[0].value,
    monitoringDetails: {
      severity: rawDataSources.monitoringSeverities[0].value,
      reaction: '',
    },
    deliverUsingHttp2: false,
    attachSubscriptionIdentityHeaders: false,
    deleteSubscriptionAutomatically: false,
  });
  const dataSources: DataSources = {
    ...rawDataSources,
    contentTypes: computed(() =>
      rawDataSources.contentTypes.filter(
        (contentType) =>
          !contentType.unsupportedDeliveryTypes.includes(
            form.value.deliveryType,
          ),
      ),
    ),
  };
  const searchTimeout = ref();
  watch(
    () => form.value.ownerSearch,
    async (searchingPhrase) => {
      const selectedOwnerSource = form.value.ownerSource;
      if (!selectedOwnerSource || !searchingPhrase) {
        return;
      }
      clearTimeout(searchTimeout.value);
      searchTimeout.value = setTimeout(async () => {
        try {
          dataSources.loadingOwners.value = true;
          dataSources.owners.value = (
            await searchOwners(selectedOwnerSource.name, searchingPhrase)
          ).data.map((source) => {
            return { title: source.name, value: source.id };
          });
        } catch (e) {
          errors.value.fetchOwners = e as Error;
        } finally {
          dataSources.loadingOwners.value = false;
        }
      }, 500);
    },
  );
  watch(
    () => form.value.deliveryType,
    () => {
      form.value.contentType = '';
    },
  );
  const validators = formValidators(form);

  const creatingSubscription = ref(false);

  async function createSubscription() {
    creatingSubscription.value = true;
    const requestBody = parseFormToRequestBody(topic, form.value);
    try {
      const response = await hermesClient.createSubscription(
        topic,
        requestBody,
      );
      console.log(response);
    } catch (e) {
      const notificationsStore = useAppNotifications();
      notificationsStore.dispatchNotification({
        title: 'Failed creating subscription',
        type: 'error',
        text: 'Error occured',
      });
    } finally {
      creatingSubscription.value = false;
    }
  }

  return {
    form,
    validators,
    dataSources,
    creatingSubscription,
    errors,
    createSubscription,
  };
}

function useDataSources(errors: Ref<UseCreateSubscriptionErrors>) {
  const configStore = useAppConfigStore();
  const contentTypes = [
    { title: 'JSON', value: 'JSON', unsupportedDeliveryTypes: [] },
    {
      title: 'AVRO (not supported in BATCH delivery mode)',
      value: 'AVRO',
      unsupportedDeliveryTypes: ['BATCH'],
    },
  ];
  const deliveryTypes = configStore.loadedConfig.subscription.deliveryTypes.map(
    (type) => {
      return {
        title: type.label,
        value: type.value,
      };
    },
  );
  const deliveryModes = [
    { title: 'ANYCAST', value: 'ANYCAST' },
    { title: 'BROADCAST (incubating feature)', value: 'BROADCAST' },
  ];
  const monitoringSeverities = [
    { title: 'Non-important', value: 'NON_IMPORTANT' },
    { title: 'Important', value: 'IMPORTANT' },
    { title: 'Critical', value: 'CRITICAL' },
  ];
  const messageDeliveryTrackingModes = [
    { title: 'No tracking', value: 'trackingOff' },
    { title: 'Track message discarding only', value: 'discardedOnly' },
    { title: 'Track everything', value: 'trackingAll' },
  ];
  const fetchedOwnerSources = ref<OwnerSource[]>([]);
  const ownerSources = computed(() =>
    fetchedOwnerSources.value
      .filter((source) => !source.deprecated)
      .map((source) => {
        return { title: source.name, value: source };
      }),
  );
  fetchOwnersSources()
    .then((response) => (fetchedOwnerSources.value = response.data))
    .catch((err) => (errors.value.fetchOwnerSources = err as Error));
  const owners = ref<SelectFieldOption[]>([]);
  const loadingOwners = ref(false);

  return {
    contentTypes,
    deliveryTypes,
    deliveryModes,
    monitoringSeverities,
    messageDeliveryTrackingModes,
    fetchedOwnerSources,
    ownerSources,
    owners,
    loadingOwners,
  };
}

function parseFormToRequestBody(
  topic: string,
  form: SubscriptionForm,
): CreateSubscriptionFormRequestBody {
  return {
    name: form.name,
    topicName: topic,
    owner: {
      source: form.ownerSource!!.name,
      id: form.owner,
    },
    contentType: form.contentType,
    deliveryType: form.deliveryType,
    description: form.description,
    endpoint: form.endpoint,
    filters: [],
    headers: [],
    http2Enabled: form.deliverUsingHttp2,
    mode: form.mode,
    monitoringDetails: {
      reaction: form.monitoringDetails.reaction,
      severity: form.monitoringDetails.severity,
    },
    subscriptionPolicy: {
      backoffMaxIntervalInSec: 600,
      backoffMultiplier: form.subscriptionPolicy.retryBackoffMultiplier,
      messageBackoff: form.subscriptionPolicy.retryBackoff,
      messageTtl: form.subscriptionPolicy.inflightMessageTTL,
      rate: form.subscriptionPolicy.rateLimit!!,
      requestTimeout: form.subscriptionPolicy.requestTimeout,
      sendingDelay: form.subscriptionPolicy.sendingDelay,
    },
    trackingMode: form.messageDeliveryTrackingMode,
  };
}
