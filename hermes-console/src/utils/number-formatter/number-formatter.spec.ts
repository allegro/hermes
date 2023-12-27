import { formatNumber } from '@/utils/number-formatter/number-formatter';

describe('number-formatter', () => {
  it.each([
    [42.21937, '42.22'],
    [42.21137, '42.21'],
    [3.1415926535, '3.14'],
    [0.001234, '0.00'],
  ])(
    'should format number with fraction part (%d)',
    (number: number, expectedFormatted: string) => {
      // when
      const formattedNumber = formatNumber(number, 2);

      // then
      expect(formattedNumber).toBe(expectedFormatted);
    },
  );

  it.each([
    [2001, '2,001'],
    [19741, '19,741'],
    [1500100900, '1,500,100,900'],
    [59454123997, '59,454,123,997'],
  ])(
    'should format number with thousands separators and no fraction part (%d)',
    (number: number, expectedFormatted: string) => {
      // when
      const formattedNumber = formatNumber(number, 0);

      // then
      expect(formattedNumber).toBe(expectedFormatted);
    },
  );

  it.each([
    [2001.94913, '2,001.949'],
    [19741.21279, '19,741.213'],
    [1500100900.001, '1,500,100,900.001'],
    [59454123997.0001, '59,454,123,997.000'],
  ])(
    'should format number with thousands separators and fraction part (%d)',
    (number: number, expectedFormatted) => {
      // when
      const formattedNumber = formatNumber(number, 3);

      // then
      expect(formattedNumber).toBe(expectedFormatted);
    },
  );
});
