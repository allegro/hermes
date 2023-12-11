import { formatTimestamp } from '@/utils/date-formatter/date-formatter';

describe('dates-utils', () => {
  it.each([
    [1, '1970-01-01 00:00:01'],
    [1234567890, '2009-02-13 23:31:30'],
    [1500100900, '2017-07-15 06:41:40'],
  ])(
    'should format ISO date from provided timestamp (%s)',
    (timestamp: number, expectedDate: string) => {
      // when
      const formattedDate = formatTimestamp(timestamp);

      // then
      expect(formattedDate).toBe(expectedDate);
    },
  );
});
