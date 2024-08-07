import {
  syncGroup as doSyncGroup,
  syncSubscription as doSyncSubscription,
  syncTopic as doSyncTopic,
} from '@/api/hermes-client';
import { ref, type Ref } from 'vue';

export interface UseSync {
  errorMessage: Ref<Error | null | undefined>;
  syncGroup: (groupName: string, primaryDatacenter: string) => Promise<void>;
  syncTopic: (
    topicQualifiedName: string,
    primaryDatacenter: string,
  ) => Promise<void>;
  syncSubscription: (
    topicQualifiedName: string,
    subscriptionName: string,
    primaryDatacenter: string,
  ) => Promise<void>;
}

export function useSync(): UseSync {
  const errorMessage: Ref<Error | null | undefined> = ref();

  const syncGroup = async (groupName: string, primaryDatacenter: string) => {
    try {
      await doSyncGroup(groupName, primaryDatacenter);
    } catch (e) {
      errorMessage.value = e as Error;
    }
  };

  const syncTopic = async (
    topicQualifiedName: string,
    primaryDatacenter: string,
  ) => {
    try {
      await doSyncTopic(topicQualifiedName, primaryDatacenter);
    } catch (e) {
      errorMessage.value = e as Error;
    }
  };

  const syncSubscription = async (
    topicQualifiedName: string,
    subscriptionName: string,
    primaryDatacenter: string,
  ) => {
    try {
      await doSyncSubscription(
        topicQualifiedName,
        subscriptionName,
        primaryDatacenter,
      );
    } catch (e) {
      errorMessage.value = e as Error;
    }
  };

  return {
    errorMessage,
    syncGroup,
    syncSubscription,
    syncTopic,
  };
}
