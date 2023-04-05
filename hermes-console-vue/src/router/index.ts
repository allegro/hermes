import { createRouter, createWebHistory } from 'vue-router';

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('../views/home/HomeView.vue'),
    },
    {
      path: '/groups/:groupId/topics/:topicId/subscriptions/:subscriptionId',
      name: 'subscription',
      component: () => import('../views/subscription/SubscriptionView.vue'),
    },
    {
      path: '/readiness/',
      name: 'readiness',
      component: () => import('../views/admin/readiness/ReadinessView.vue'),
    },
    {
      path: '/constraints/',
      name: 'constraints',
      component: () => import('../views/admin/constraints/ConstraintsView.vue'),
    },
    {
      path: '/consistency/',
      name: 'consistency',
      component: () => import('../views/admin/consistency/ConsistencyView.vue'),
    },
    {
      path: '/groups/:groupId/topics/:topicId/subscriptions/:subscriptionId/diagnostics/',
      name: 'consumerGroups',
      component: () =>
        import('../views/admin/consumer-groups/ConsumerGroupsView.vue'),
    },
  ],
});

export default router;
