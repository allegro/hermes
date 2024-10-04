import { computed, ref } from 'vue';
import { defaultMaxMessageSize } from '@/composables/topic/use-create-topic/useCreateTopic';
import { dummyAppConfig } from '@/dummy/app-config';
import { dummyOwner, dummyTopic } from '@/dummy/topic';
import { matchRegex, max, min, required } from '@/utils/validators';
import type { DataSources } from '@/composables/topic/use-form-topic/types';

export const dummyTopicForm = {
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
};

export const dummyTopicFormValidator = {
  name: [required(), matchRegex(/^[a-zA-Z0-9.-]+$/, 'Invalid name')],
  description: [required()],
  ownerSource: [required()],
  owner: [required()],
  contentType: [required()],
  retentionTimeDurationDays: [required(), min(1), max(7)],
  retentionTimeDurationHours: [required(), min(1), max(24)],
  maxMessageSize: [required(), min(0)],
  offlineRetentionTime: [required(), min(0)],
  ack: [required()],
};

export const dummyContentTypes = [
  {
    title: 'AVRO',
    value: 'AVRO',
  },
];

export const dummyAckModes = [
  { title: 'LEADER', value: 'LEADER' },
  { title: 'ALL', value: 'ALL' },
];

export const dummyRetentionUnits = [
  { title: 'DAYS', value: 'DAYS' },
  { title: 'HOURS', value: 'HOURS' },
];

export const dummyOwnerSources = [
  {
    name: 'Service Catalog',
    autocomplete: true,
    deprecated: false,
  },
];

export const dummyDataSources: DataSources = {
  contentTypes: dummyContentTypes,
  ackModes: dummyAckModes,
  retentionUnits: dummyRetentionUnits,
  ownerSources: computed(() =>
    dummyOwnerSources
      .filter((source) => !source.deprecated)
      .map((source) => {
        return { title: source.name, value: source };
      }),
  ),
  owners: ref(
    [dummyOwner].map((source) => {
      return { title: source.name, value: source.id };
    }),
  ),
  loadingOwners: ref(false),
};

export const dummyInitializedTopicForm = {
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
    retentionUnit: dummyAppConfig.topic.defaults.retentionTime.retentionUnit,
    infinite: false,
    duration: dummyAppConfig.topic.defaults.retentionTime.duration,
  },
  offlineStorage: {
    enabled: dummyAppConfig.topic.defaults.offlineStorage.enabled,
    retentionTime: {
      retentionUnit:
        dummyAppConfig.topic.defaults.offlineStorage.retentionTime
          .retentionUnit,
      infinite: false,
      duration:
        dummyAppConfig.topic.defaults.offlineStorage.retentionTime.duration,
    },
  },
  trackingEnabled: false,
  contentType: dummyAppConfig.topic.defaults.contentType,
  maxMessageSize: defaultMaxMessageSize,
  ack: '',
  schema: '',
};

export const dummyInitializedEditTopicForm = {
  name: dummyTopic.name,
  description: dummyTopic.description,
  ownerSource: null,
  owner: dummyTopic.owner.id,
  ownerSearch: '',
  auth: {
    enabled: dummyTopic.auth.enabled,
    unauthenticatedAccessEnabled: dummyTopic.auth.unauthenticatedAccessEnabled,
    publishers: dummyTopic.auth.publishers
      ? dummyTopic.auth.publishers.join(',')
      : '',
  },
  subscribingRestricted: dummyTopic.subscribingRestricted,
  retentionTime: {
    retentionUnit: dummyTopic.retentionTime.retentionUnit,
    infinite: false,
    duration: dummyTopic.retentionTime.duration,
  },
  offlineStorage: {
    enabled: dummyTopic.offlineStorage.enabled,
    retentionTime: {
      retentionUnit: 'DAYS',
      infinite: dummyTopic.offlineStorage.retentionTime.infinite,
      duration: dummyTopic.offlineStorage.retentionTime.duration,
    },
  },
  trackingEnabled: dummyTopic.trackingEnabled,
  contentType: dummyTopic.contentType,
  maxMessageSize: dummyTopic.maxMessageSize!!,
  ack: dummyTopic.ack,
  schema: dummyTopic.schema ? dummyTopic.schema : '',
};
