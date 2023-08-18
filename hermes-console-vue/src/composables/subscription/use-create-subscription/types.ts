import type { ComputedRef, Ref } from 'vue';
import type { FieldValidator } from '@/utils/validators';
import type { HeaderFilter } from '@/views/subscription/subscription-form/subscription-header-filters/types';
import type { OwnerSource } from '@/api/owner';
import type { PathFilter } from '@/views/subscription/subscription-form/subscription-basic-filters/types';
import type { SelectFieldOption } from '@/components/select-field/types';

export interface UseCreateSubscription {
  form: Ref<SubscriptionForm>;
  validators: FormValidators;
  dataSources: DataSources;
  creatingSubscription: Ref<boolean>;
  errors: Ref<UseCreateSubscriptionErrors>;
  createSubscription: () => Promise<void>;
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
  retryOn4xx: boolean;
  messageDeliveryTrackingMode: string;
  monitoringDetails: FormMonitoringDetails;
  deliverUsingHttp2: boolean;
  attachSubscriptionIdentityHeaders: boolean;
  deleteSubscriptionAutomatically: boolean;
  pathFilters: PathFilter[];
  headerFilters: HeaderFilter[];
}

export interface FormSubscriptionPolicy {
  rateLimit: number | null;
  inflightMessageTTL: number;
  retryBackoff: number;
  sendingDelay: number;
  retryBackoffMultiplier: number;
  requestTimeout: number;
  batchSize: number | null;
  batchTime: number | null;
  batchVolume: number | null;
}

export interface FormMonitoringDetails {
  severity: string;
  reaction: string;
}

export interface FormValidators {
  name: FieldValidator<string>[];
  endpoint: FieldValidator<string>[];
  description: FieldValidator<string>[];
  ownerSource: FieldValidator<string>[];
  owner: FieldValidator<any>[];
  contentType: FieldValidator<string>[];
  deliveryType: FieldValidator<string>[];
  mode: FieldValidator<string>[];
  rateLimit: FieldValidator<number>[];
  batchSize: FieldValidator<number>[];
  batchTime: FieldValidator<number>[];
  batchVolume: FieldValidator<number>[];
  requestTimeout: FieldValidator<number>[];
  sendingDelay: FieldValidator<number>[];
  inflightMessageTTL: FieldValidator<number>[];
  retryBackoff: FieldValidator<number>[];
  retryBackoffMultiplier: FieldValidator<number>[];
  messageDeliveryTrackingMode: FieldValidator<string>[];
  monitoringSeverity: FieldValidator<string>[];
}

export interface UseCreateSubscriptionErrors {
  fetchOwnerSources: Error | null;
  fetchOwners: Error | null;
}

export interface DataSources {
  contentTypes: ComputedRef<SelectFieldOption[]>;
  deliveryTypes: SelectFieldOption[];
  deliveryModes: SelectFieldOption[];
  monitoringSeverities: SelectFieldOption[];
  messageDeliveryTrackingModes: SelectFieldOption[];
  ownerSources: ComputedRef<SelectFieldOption<OwnerSource>[]>;
  owners: Ref<SelectFieldOption[]>;
  loadingOwners: Ref<boolean>;
}
