import type { Group } from '@/composables/use-groups/useGroups';

export const dummyGroupNames = [
  'pl.allegro.public.offer',
  'pl.allegro.public.offer.product',
  'pl.allegro.public.order',
  'pl.allegro.public.user',
  'pl.allegro.public.group',
  'pl.allegro.public.admin',
];

export const dummyGroups: Group[] = [
  {
    name: 'pl.allegro.offer',
    topics: ['pl.allegro.offer.OfferEventV1'],
  },
  {
    name: 'pl.allegro.offer.product',
    topics: [
      'pl.allegro.offer.product.ProductEventV1',
      'pl.allegro.offer.product.ProductEventV2',
    ],
  },
  ...dummyGroupNames.slice(2).map((name) => ({ name, topics: [] })),
];
