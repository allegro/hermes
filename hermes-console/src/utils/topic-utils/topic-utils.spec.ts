import { describe, expect } from 'vitest';
import { groupName } from '@/utils/topic-utils/topic-utils';

describe('topics-utils', () => {
  it('should retrieve group name from topic name', () => {
    const actual = groupName('pl.allegro.public.group.DummyEvent');

    expect(actual).toEqual('pl.allegro.public.group');
  });
});
