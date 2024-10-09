import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '@/store/auth/useAuthStore';
import axios from '@/utils/axios/axios-instance';

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
      path: '/ui/favorite-topics',
      name: 'favoriteTopics',
      component: () => import('@/views/favorite/topics/FavoriteTopicsView.vue'),
    },
    {
      path: '/ui/favorite-subscriptions',
      name: 'favoriteSubscriptions',
      component: () =>
        import('@/views/favorite/subscriptions/FavoriteSubscriptionsView.vue'),
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
      path: '/ui/consistency/:groupId',
      name: 'groupConsistency',
      component: () =>
        import(
          '@/views/admin/consistency/inconsistent-group/InconsistentGroup.vue'
        ),
    },
    {
      path: '/ui/consistency/:groupId/topics/:topicId',
      name: 'topicConsistency',
      component: () =>
        import(
          '@/views/admin/consistency/inconsistent-topic/InconsistentTopic.vue'
        ),
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
    {
      path: '/ui/search',
      name: 'search',
      component: () => import('@/views/search/SearchView.vue'),
      props: (route) => ({
        collection: route.query.collection,
        filter: route.query.filter,
        pattern: route.query.pattern,
      }),
    },
  ],
});

router.beforeEach(() => {
  const authStore = useAuthStore();

  if (authStore.isUserAuthorized) {
    axios.interceptors.request.use(function (config) {
      config.headers.Authorization = `Bearer ${authStore.accessToken}`;
      return config;
    });
  }
});

export default router;
