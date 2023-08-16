import { computed, ref, watch } from 'vue';
import { fetchOwnersSources, searchOwners } from '@/api/hermes-client';
import { matchRegex, required } from '@/utils/validators';
import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
import type { ComputedRef, Ref } from 'vue';
import type { FieldValidator } from '@/utils/validators';
import type { OwnerSource } from '@/api/owner';
import type { SelectFieldOption } from '@/views/subscription/subscription-form/select-field/types';

export interface UseCreateSubscription {
  form: Ref<SubscriptionForm>;
  validators: FormValidators;
  dataSources: DataSources;
  errors: Ref<UseCreateSubscriptionErrors>;
}

export interface SubscriptionForm {
  name: string;
  endpoint: string;
  description: string;
  ownerSource: OwnerSource | null;
  owner: string;
  ownerSearch: string;
  contentType: string;
  deliveryType: string;
  subscriptionPolicy: FormSubscriptionPolicy;
  mode: string;
  rateLimit: number | null;
  retryOn4xx: boolean;
  messageDeliveryTrackingMode: string;
  monitoringDetails: FormMonitoringDetails;
  deliverUsingHttp2: boolean;
  attachSubscriptionIdentityHeaders: boolean;
  deleteSubscriptionAutomatically: boolean;
}

interface FormSubscriptionPolicy {
  inflightMessageTTL: number;
  retryBackoff: number;
  sendingDelay: number;
  retryBackoffMultiplier: number;
  requestTimeout: number;
}

interface FormMonitoringDetails {
  severity: string;
  reaction: string;
}

interface FormValidators {
  name: FieldValidator<string>[];
  endpoint: FieldValidator<string>[];
  description: FieldValidator<string>[];
  ownerSource: FieldValidator<string>[];
  owner: FieldValidator<any>[];
  contentType: FieldValidator<string>[];
  deliveryType: FieldValidator<string>[];
}

interface UseCreateSubscriptionErrors {
  fetchOwnerSources: Error | null;
  fetchOwners: Error | null;
}

interface DataSources {
  contentTypes: ComputedRef<SelectFieldOption[]>;
  deliveryTypes: SelectFieldOption[];
  deliveryModes: SelectFieldOption[];
  monitoringSeverities: SelectFieldOption[];
  messageDeliveryTrackingModes: SelectFieldOption[];
  ownerSources: ComputedRef<SelectFieldOption<OwnerSource>[]>;
  owners: Ref<SelectFieldOption[]>;
  loadingOwners: Ref<boolean>;
}

const formValidators: FormValidators = {
  name: [required(), matchRegex(/^[a-zA-Z0-9.-]+$/, 'Invalid name')],
  endpoint: [required()],
  description: [required()],
  ownerSource: [required()],
  owner: [required()],
  contentType: [required()],
  deliveryType: [required()],
};

export function useCreateSubscription(): UseCreateSubscription {
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
    rateLimit: null,
    subscriptionPolicy: {
      inflightMessageTTL:
        loadedConfig.subscription.defaults.subscriptionPolicy.messageTtl ||
        3600,
      retryBackoff: 1000,
      retryBackoffMultiplier: 1.0,
      sendingDelay: 0,
      requestTimeout:
        loadedConfig.subscription.defaults.subscriptionPolicy.requestTimeout ||
        1000,
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

  return {
    form,
    validators: formValidators,
    dataSources,
    errors,
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
