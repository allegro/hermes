import { createRouter, createWebHistory } from 'vue-router';

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
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
      path: '/groups/:groupId/topics/:topicId/subscriptions/:subscriptionId/',
      name: 'subscription',
      component: () => import('../views/subscription/SubscriptionView.vue'),
    },
    {
      path: '/groups/:groupId/topics/:topicId',
      name: 'topic',
      component: () => import('../views/topic/TopicView.vue'),
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'home',
      component: () => import('../views/home/HomeView.vue'),
    },
  ],
});

export default router;
