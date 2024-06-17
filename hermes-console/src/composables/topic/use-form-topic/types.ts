import type { ComputedRef } from 'vue';
import type { FieldValidator } from '@/utils/validators';
import type { OwnerSource } from '@/api/owner';
import type { Ref } from 'vue';
import type { SelectFieldOption } from '@/components/select-field/types';

export interface UseFormTopic {
  form: Ref<TopicForm>;
  validators: FormValidators;
  dataSources: DataSources;
}

export interface TopicForm {
  name: string;
  description: string;
  ownerSource?: OwnerSource | null;
  owner: string;
  ownerSearch?: string;
  auth: FormAuthPolicy;
  subscribingRestricted: boolean;
  retentionTime: FormRetentionTime;
  offlineStorage: FormOfflineStorage;
  trackingEnabled: boolean;
  contentType: string;
  maxMessageSize: number;
  ack: string;
  schema?: string;
}

export interface FormAuthPolicy {
  enabled: boolean;
  unauthenticatedAccessEnabled: boolean;
  publishers: string;
}

export interface FormRetentionTime {
  retentionUnit?: string;
  infinite?: boolean | null;
  duration: number;
}

export interface FormOfflineStorage {
  retentionTime: FormRetentionTime;
  enabled: boolean;
}

export interface FormValidators {
  name: FieldValidator<string>[];
  description: FieldValidator<string>[];
  ownerSource: FieldValidator<string>[];
  owner: FieldValidator<any>[];
  contentType: FieldValidator<string>[];
  retentionTimeDuration: FieldValidator<number>[];
  maxMessageSize: FieldValidator<number>[];
  offlineRetentionTime: FieldValidator<number>[];
  ack: FieldValidator<string>[];
}

export interface RawDataSources {
  contentTypes: SelectFieldOption[];
  ackModes: SelectFieldOption[];
  retentionUnits: SelectFieldOption[];
  fetchedOwnerSources: any;
  ownerSources: ComputedRef<SelectFieldOption<OwnerSource>[]>;
  owners: Ref<SelectFieldOption[]>;
  loadingOwners: Ref<boolean>;
}

export interface DataSources {
  contentTypes: SelectFieldOption[];
  ackModes: SelectFieldOption[];
  retentionUnits: SelectFieldOption[];
  ownerSources: ComputedRef<SelectFieldOption<OwnerSource>[]>;
  owners: Ref<SelectFieldOption[]>;
  loadingOwners: Ref<boolean>;
}
