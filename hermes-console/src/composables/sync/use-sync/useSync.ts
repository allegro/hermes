import { ref, type Ref } from 'vue';
import { syncGroups, syncSubscriptions, syncTopics } from '@/api/hermes-client';

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
      await syncGroups(groupName, primaryDatacenter);
    } catch (e) {
      errorMessage.value = e as Error;
    }
  };

  const syncTopic = async (
    topicQualifiedName: string,
    primaryDatacenter: string,
  ) => {
    try {
      await syncTopics(topicQualifiedName, primaryDatacenter);
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
      await syncSubscriptions(
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
