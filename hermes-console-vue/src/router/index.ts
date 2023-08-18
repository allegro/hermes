import { createRouter, createWebHistory } from 'vue-router';

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: () => '/ui',
    },
    {
      path: '/ui',
      name: 'home',
      component: () => import('@/views/home/HomeView.vue'),
    },
    {
      path: '/ui/groups',
      name: 'groups',
      component: () => import('@/views/groups/GroupsView.vue'),
    },
    {
      path: '/ui/groups/:groupId',
      name: 'groupTopics',
      component: () => import('@/views/group-topics/GroupTopicsView.vue'),
    },
    {
      path: '/ui/groups/:groupId/topics/:topicName',
      name: 'topic',
      component: () => import('@/views/topic/TopicView.vue'),
    },
    {
      path: '/ui/groups/:groupId/topics/:topicId/subscriptions/:subscriptionId',
      name: 'subscription',
      component: () => import('@/views/subscription/SubscriptionView.vue'),
    },
    {
      path: '/ui/readiness',
      name: 'readiness',
      component: () => import('@/views/admin/readiness/ReadinessView.vue'),
    },
    {
      path: '/ui/constraints',
      name: 'constraints',
      component: () => import('@/views/admin/constraints/ConstraintsView.vue'),
    },
    {
      path: '/ui/consistency',
      name: 'consistency',
      component: () => import('@/views/admin/consistency/ConsistencyView.vue'),
    },
    {
      path: '/ui/groups/:groupId/topics/:topicId/subscriptions/:subscriptionId/diagnostics/',
      name: 'consumerGroups',
      component: () =>
        import('@/views/admin/consumer-groups/ConsumerGroupsView.vue'),
    },
    {
      path: '/ui/stats',
      name: 'stats',
      component: () => import('@/views/stats/StatsView.vue'),
    },
    {
      path: '/ui/redirect',
      name: 'redirect',
      component: () => import('@/views/redirect/RedirectView.vue'),
    },
  ],
});

export default router;
