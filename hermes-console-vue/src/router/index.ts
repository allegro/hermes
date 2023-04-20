import { createRouter, createWebHistory } from 'vue-router';

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/consistency/',
      name: 'consistency',
      component: () => import('../views/admin/consistency/ConsistencyView.vue'),
    },
    {
      path: '/constraints/',
      name: 'constraints',
      component: () => import('../views/admin/constraints/ConstraintsView.vue'),
    },
    {
      path: '/groups/',
      name: 'groups',
      component: () => import('../views/groups/GroupsView.vue'),
    },
    {
      path: '/groups/:groupId/',
      name: 'groupTopics',
      component: () => import('../views/group-topics/GroupTopicsView.vue'),
    },
    {
      path: '/groups/:groupId/topics/:topicId',
      name: 'topic',
      component: () => import('../views/topic/TopicView.vue'),
    },
    {
      path: '/groups/:groupId/topics/:topicId/subscriptions/:subscriptionId/',
      name: 'subscription',
      component: () => import('../views/subscription/SubscriptionView.vue'),
    },
    {
      path: '/groups/:groupId/topics/:topicId/subscriptions/:subscriptionId/diagnostics/',
      name: 'consumerGroups',
      component: () =>
        import('../views/admin/consumer-groups/ConsumerGroupsView.vue'),
    },
    {
      path: '/readiness/',
      name: 'readiness',
      component: () => import('../views/admin/readiness/ReadinessView.vue'),
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'home',
      component: () => import('../views/home/HomeView.vue'),
    },
  ],
});

export default router;
