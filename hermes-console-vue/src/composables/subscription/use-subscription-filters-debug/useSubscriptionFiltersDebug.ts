import { ContentType } from '@/api/content-type';
import { dispatchErrorNotification } from '@/utils/notification-utils';
import { fetchTopic, verifyFilters } from '@/api/hermes-client';
import { ref } from 'vue';
import { useGlobalI18n } from '@/i18n';
import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';
import { VerificationStatus } from '@/api/message-filters-verification';
import type { PathFilter } from '@/views/subscription/subscription-form/subscription-basic-filters/types';
import type { PathFilterJson } from '@/api/subscription';
import type { Ref } from 'vue';

export interface UseSubscriptionFiltersDebug {
  status: Ref<VerificationStatus | undefined>;
  errorMessage: Ref<string | undefined>;
  fetchContentType: (topicName: string) => Promise<FetchTopicContentType>;
  verify: (
    topicName: string,
    filters: PathFilter[],
    message: string,
    topicContentType: ContentType,
  ) => Promise<void>;
}

export interface FetchTopicContentType {
  contentType: ContentType | undefined;
  error: Error | null;
}

const toFiltersJSON = (
  filter: PathFilter,
  type: ContentType,
): PathFilterJson => {
  return {
    path: filter.path,
    type: type === ContentType.JSON ? 'jsonpath' : 'avropath',
    matcher: filter.matcher,
    matchingStrategy: filter.matchingStrategy,
  };
};

export function useSubscriptionFiltersDebug(): UseSubscriptionFiltersDebug {
  const notificationStore = useNotificationsStore();
  const status: Ref<VerificationStatus | undefined> = ref();
  const errorMessage: Ref<string | undefined> = ref();

  const fetchContentType = async (
    topicName: string,
  ): Promise<FetchTopicContentType> => {
    try {
      const topicContentType = (await fetchTopic(topicName)).data.contentType;
      return {
        contentType: topicContentType,
        error: null,
      };
    } catch (e: any) {
      return {
        contentType: undefined,
        error: e as Error,
      };
    }
  };

  const verify = async (
    topicName: string,
    filters: PathFilter[],
    message: string,
    topicContentType: ContentType,
  ) => {
    try {
      const filtersJSON = filters.map((f) =>
        toFiltersJSON(f, topicContentType),
      );
      const response = (
        await verifyFilters(topicName, {
          message: btoa(message),
          filters: filtersJSON,
        })
      ).data;
      if (response.status == VerificationStatus.ERROR) {
        errorMessage.value = response.errorMessage;
      }
      status.value = response.status;
    } catch (e: any) {
      dispatchErrorNotification(
        e,
        notificationStore,
        useGlobalI18n().t(
          'notifications.subscriptionFiltersDebug.verification.failure',
        ),
      );
    }
  };

  return {
    status,
    errorMessage,
    fetchContentType,
    verify,
  };
}
