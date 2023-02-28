import { render } from '@/utils/test-utils';
import SubscriptionBreadcrumbs from '@/views/subscription/subscription-breadcrumbs/SubscriptionBreadcrumbs.vue';

describe('SubscriptionBreadcrumbs', () => {
  const props = {
    groupId: 'pl.allegro.public.group',
    topicId: 'pl.allegro.public.group.DummyEvent',
    subscriptionId: 'foobar-service',
  };

  it('should render `home` breadcrumb with an anchor to a home page', () => {
    // given
    const { getByText } = render(SubscriptionBreadcrumbs, { props });

    // when
    const element = getByText(
      'subscription.subscriptionBreadcrumbs.home',
    ) as HTMLAnchorElement;

    // then
    expect(element).toHaveAttribute('href', '/');
  });

  it('should render `groups` breadcrumb with an anchor to a group listing', () => {
    // given
    const { getByText } = render(SubscriptionBreadcrumbs, { props });

    // when
    const element = getByText(
      'subscription.subscriptionBreadcrumbs.groups',
    ) as HTMLAnchorElement;

    // then
    expect(element).toHaveAttribute('href', '/groups/');
  });

  it('should render a breadcrumb with an anchor to topics listing', () => {
    // given
    const { getByText } = render(SubscriptionBreadcrumbs, { props });

    // when
    const element = getByText('pl.allegro.public.group') as HTMLAnchorElement;

    // then
    expect(element).toHaveAttribute('href', '/groups/pl.allegro.public.group/');
  });

  it('should render a breadcrumb with an anchor to topic page', () => {
    // given
    const { getByText } = render(SubscriptionBreadcrumbs, { props });

    // when
    const element = getByText(
      'pl.allegro.public.group.DummyEvent',
    ) as HTMLAnchorElement;

    // then
    expect(element).toHaveAttribute(
      'href',
      '/groups/pl.allegro.public.group/topics/pl.allegro.public.group.DummyEvent/',
    );
  });
});
