import { render } from '@/utils/test-utils';
import ConsumerGroupsBreadcrumbs from '@/views/admin/consumer-groups/consumer-groups-breadcrumbs/ConsumerGroupsBreadcrumbs.vue';

describe('ConsumerGroupsBreadcrumbs', () => {
  const props = {
    groupId: 'pl.allegro.public.group',
    topicId: 'pl.allegro.public.group.DummyEvent',
    subscriptionId: 'foobar-service',
  };

  it('should render `home` breadcrumb with an anchor to a home page', () => {
    // given
    const { getByText } = render(ConsumerGroupsBreadcrumbs, { props });

    // when
    const element = getByText(
      'consumerGroups.breadcrumbs.home',
    ) as HTMLAnchorElement;

    // then
    expect(element).toHaveAttribute('href', '/');
  });

  it('should render `groups` breadcrumb with an anchor to a group listing', () => {
    // given
    const { getByText } = render(ConsumerGroupsBreadcrumbs, { props });

    // when
    const element = getByText(
      'consumerGroups.breadcrumbs.groups',
    ) as HTMLAnchorElement;

    // then
    expect(element).toHaveAttribute('href', '/groups');
  });

  it('should render a breadcrumb with an anchor to topics listing', () => {
    // given
    const { getByText } = render(ConsumerGroupsBreadcrumbs, { props });

    // when
    const element = getByText('pl.allegro.public.group') as HTMLAnchorElement;

    // then
    expect(element).toHaveAttribute('href', '/groups/pl.allegro.public.group');
  });

  it('should render a breadcrumb with an anchor to topic page', () => {
    // given
    const { getByText } = render(ConsumerGroupsBreadcrumbs, { props });

    // when
    const element = getByText(
      'pl.allegro.public.group.DummyEvent',
    ) as HTMLAnchorElement;

    // then
    expect(element).toHaveAttribute(
      'href',
      '/groups/pl.allegro.public.group/topics/pl.allegro.public.group.DummyEvent',
    );
  });

  it('should render a breadcrumb with an anchor to subscription page', () => {
    // given
    const { getByText } = render(ConsumerGroupsBreadcrumbs, { props });

    // when
    const element = getByText('foobar-service') as HTMLAnchorElement;

    // then
    expect(element).toHaveAttribute(
      'href',
      '/groups/pl.allegro.public.group/topics/pl.allegro.public.group.DummyEvent/subscriptions/foobar-service',
    );
  });
});
