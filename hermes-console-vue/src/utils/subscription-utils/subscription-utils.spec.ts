import { describe, expect } from 'vitest';
import {
  parseSubscriptionFqn,
  subscriptionFqn,
} from '@/utils/subscription-utils/subscription-utils';

import type { SubscriptionName } from '@/utils/subscription-utils/subscription-utils';
describe('topics-utils', () => {
  it('should retrieve topic name and subscription name from subscription fully qualified name', () => {
    // given
    const subscriptionFqn = 'pl.allegro.public.group.DummyEvent$subscription';

    // when
    const actual = parseSubscriptionFqn(subscriptionFqn);

    // then
    const expected: SubscriptionName = {
      topicName: 'pl.allegro.public.group.DummyEvent',
      subscriptionName: 'subscription',
    };
    expect(actual).toEqual(expected);
  });

  it('should throw error when subscription fully qualified name is invalid', () => {
    // given
    const fqns = [
      'pl.allegro.public.group.DummyEvent',
      'pl.allegro.public.group.DummyEvent$sub$sub',
    ];

    // when & then
    fqns.forEach((fqn) =>
      expect(() => parseSubscriptionFqn(fqn)).toThrowError(),
    );
  });

  it('should create subscription fully qualified name from topic name and subscription name', () => {
    // given
    const topicName = 'pl.allegro.public.group.DummyEvent';
    const subscriptionName = 'subscription';

    // when
    const fqn = subscriptionFqn(topicName, subscriptionName);

    // then
    expect(fqn).toEqual('pl.allegro.public.group.DummyEvent$subscription');
  });
});
