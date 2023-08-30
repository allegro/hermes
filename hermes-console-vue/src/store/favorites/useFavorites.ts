import { defineStore } from 'pinia';
import { subscriptionFqn } from '@/utils/subscription-utils/subscription-utils';
import type { FavoritesState } from '@/store/favorites/types';

export const useFavorites = defineStore('favorites', {
  state: (): FavoritesState => {
    return {
      topics: [],
      subscriptions: [],
    };
  },
  actions: {
    async addTopic(topic: string) {
      this.topics.push(topic);
    },
    async addSubscription(topic: string, subscription: string) {
      this.subscriptions.push(subscriptionFqn(topic, subscription));
    },
    async removeTopic(topic: string) {
      this.topics = this.topics.filter((topicName) => topicName !== topic);
    },
    async removeSubscription(topic: string, subscription: string) {
      const subscriptionQualifiedName = subscriptionFqn(topic, subscription);
      this.subscriptions = this.subscriptions.filter(
        (subscriptionName) => subscriptionName !== subscriptionQualifiedName,
      );
    },
    async removeSubscriptionByQualifiedName(subscriptionQualifiedName: string) {
      this.subscriptions = this.subscriptions.filter(
        (subscriptionName) => subscriptionName !== subscriptionQualifiedName,
      );
    },
  },
  getters: {
    getTopics(state: FavoritesState): string[] {
      return state.topics?.sort((a, b) => a.localeCompare(b));
    },
    getSubscriptions(state: FavoritesState): string[] {
      return state.subscriptions?.sort((a, b) => a.localeCompare(b));
    },
  },
  persist: true,
});
