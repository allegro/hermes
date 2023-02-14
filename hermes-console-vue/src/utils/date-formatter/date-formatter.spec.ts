import { dateFromTimestamp } from '@/utils/date-formatter/date-formatter';

describe('dates-utils', () => {
  it.each([
    { timestamp: 1, expectedDate: '1970-01-01 00:00:01' },
    { timestamp: 1234567890, expectedDate: '2009-02-13 23:31:30' },
    { timestamp: 1500100900, expectedDate: '2017-07-15 06:41:40' },
  ])(
    'should format ISO date from provided timestamp (%s)',
    ({ timestamp, expectedDate }) => {
      // when
      const formattedDate = dateFromTimestamp(timestamp);

      // then
      expect(formattedDate).toBe(expectedDate);
    },
  );
});
