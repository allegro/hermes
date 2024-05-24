import { ContentType } from '@/api/content-type';
import { dispatchErrorNotification } from '@/utils/notification-utils';
import { fetchContentType } from '@/composables/topic/use-topic/useTopic';
import { ref } from 'vue';
import { useGlobalI18n } from '@/i18n';
import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';
import { VerificationStatus } from '@/api/message-filters-verification';
import { verifyFilters } from '@/api/hermes-client';
import type { FetchTopicContentType } from '@/composables/topic/use-topic/useTopic';
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

// https://stackoverflow.com/a/30106551
function b64EncodeUnicode(str: string): string {
  return btoa(
    encodeURIComponent(str).replace(/%([0-9A-F]{2})/g, function (match, p1) {
      return String.fromCharCode(parseInt(p1, 16));
    }),
  );
}

export function useSubscriptionFiltersDebug(): UseSubscriptionFiltersDebug {
  const notificationStore = useNotificationsStore();
  const status: Ref<VerificationStatus | undefined> = ref();
  const errorMessage: Ref<string | undefined> = ref();

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
          message: b64EncodeUnicode(message),
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
