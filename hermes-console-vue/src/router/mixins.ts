import router from '@/router/index';
import type { NavigationFailure } from 'vue-router';

export const goToHomeView = (): Promise<
  void | NavigationFailure | undefined
> => {
  return router.push({ name: 'home' });
};

export const goToTopicView = (
  groupId: string,
  topicId: string,
): Promise<void | NavigationFailure | undefined> => {
  return router.push({
    name: 'topic',
    params: { groupId: groupId, topicId: topicId },
  });
};

export const goToSubscriptionView = (
  groupId: string,
  topicId: string,
  subscriptionId: string,
): Promise<void | NavigationFailure | undefined> => {
  return router.push({
    name: 'subscription',
    params: {
      groupId: groupId,
      topicId: topicId,
      subscriptionId: subscriptionId,
    },
  });
};
