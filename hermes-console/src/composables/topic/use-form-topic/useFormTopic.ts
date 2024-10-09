import { computed, ref } from 'vue';
import { fetchOwnersSources, searchOwners } from '@/api/hermes-client';
import { matchRegex, max, min, required } from '@/utils/validators';
import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
import { watch } from 'vue';
import type {
  DataSources,
  FormValidators,
  RawDataSources,
  TopicForm,
  UseFormTopic,
} from '@/composables/topic/use-form-topic/types';
import type { OwnerSource } from '@/api/owner';
import type { Ref } from 'vue';
import type { SelectFieldOption } from '@/components/select-field/types';
import type { TopicWithSchema } from '@/api/topic';
import type { UseCreateTopicErrors } from '@/composables/topic/use-create-topic/types';

export function useFormTopic(): UseFormTopic {
  const form = createEmptyForm();
  const validators = formValidators();
  const rawDataSources = getRawDataSources();
  const dataSources = {
    ...rawDataSources,
  };

  return {
    form,
    validators,
    dataSources,
  };
}

function formValidators(): FormValidators {
  return {
    name: [required(), matchRegex(/^[a-zA-Z0-9.-]+$/, 'Invalid name')],
    description: [required()],
    ownerSource: [required()],
    owner: [required()],
    contentType: [required()],
    retentionTimeDurationDays: [required(), min(1), max(7)],
    retentionTimeDurationHours: [required(), min(1), max(24)],
    maxMessageSize: [required(), min(0)],
    offlineRetentionTime: [required(), min(1)],
    ack: [required()],
  };
}

function getRawDataSources(): RawDataSources {
  const configStore = useAppConfigStore();
  const ackModes = [
    { title: 'LEADER', value: 'LEADER' },
    { title: 'ALL', value: 'ALL' },
  ];
  const retentionUnits = [
    { title: 'DAYS', value: 'DAYS' },
    { title: 'HOURS', value: 'HOURS' },
  ];
  const contentTypes = configStore.loadedConfig.topic.contentTypes.map(
    (contentType) => {
      return { title: contentType.label, value: contentType.value };
    },
  );
  const fetchedOwnerSources = ref<OwnerSource[]>([]);
  const ownerSources = computed(() =>
    fetchedOwnerSources.value
      .filter((source) => !source.deprecated)
      .map((source) => {
        return { title: source.name, value: source };
      }),
  );
  fetchOwnersSources().then(
    (response) => (fetchedOwnerSources.value = response.data),
  );
  const owners = ref<SelectFieldOption[]>([]);
  const loadingOwners = ref(false);

  return {
    contentTypes,
    ackModes,
    retentionUnits,
    fetchedOwnerSources,
    ownerSources,
    owners,
    loadingOwners,
  };
}

function createEmptyForm(): Ref<TopicForm> {
  return ref({
    name: '',
    description: '',
    ownerSource: null,
    owner: '',
    ownerSearch: '',
    auth: {
      enabled: false,
      unauthenticatedAccessEnabled: false,
      publishers: '',
    },
    subscribingRestricted: false,
    retentionTime: {
      retentionUnit: '',
      infinite: false,
      duration: 1,
    },
    offlineStorage: {
      enabled: false,
      retentionTime: {
        retentionUnit: '',
        infinite: false,
        duration: 1,
      },
    },
    trackingEnabled: false,
    contentType: '',
    maxMessageSize: 1,
    ack: '',
    schema: '',
  });
}

export function initializeFullyFilledForm(
  form: Ref<TopicForm>,
  topic: TopicWithSchema,
): void {
  form.value = {
    name: topic.name,
    description: topic.description,
    ownerSource: null,
    owner: topic.owner.id,
    ownerSearch: '',
    auth: {
      enabled: topic.auth.enabled,
      unauthenticatedAccessEnabled: topic.auth.unauthenticatedAccessEnabled,
      publishers: topic.auth.publishers ? topic.auth.publishers.join(',') : '',
    },
    subscribingRestricted: topic.subscribingRestricted,
    retentionTime: {
      retentionUnit: topic.retentionTime.retentionUnit,
      infinite: false,
      duration: topic.retentionTime.duration,
    },
    offlineStorage: {
      enabled: topic.offlineStorage.enabled,
      retentionTime: {
        retentionUnit: 'DAYS',
        infinite: topic.offlineStorage.retentionTime.infinite,
        duration: topic.offlineStorage.retentionTime.duration,
      },
    },
    trackingEnabled: topic.trackingEnabled,
    contentType: topic.contentType,
    maxMessageSize: topic.maxMessageSize!!,
    ack: topic.ack,
    schema: topic.schema ? topic.schema : '',
  };
}

export function watchOwnerSearch(
  form: Ref<TopicForm>,
  dataSources: DataSources,
  errors: Ref<UseCreateTopicErrors>,
) {
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
}
