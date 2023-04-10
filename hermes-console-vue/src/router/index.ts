import { createRouter, createWebHistory } from 'vue-router';

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/home/HomeView.vue'),
    },
    {
      path: '/groups/:groupId/topics/:topicName',
      name: 'topic',
      component: () => import('@/views/topic/TopicView.vue'),
    },
    {
      path: '/groups/:groupId/topics/:topicName/subscriptions/:subscriptionId',
      name: 'subscription',
      component: () => import('../views/subscription/SubscriptionView.vue'),
    },
  ],
});

export default router;
