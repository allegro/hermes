import { deepSearch, isJsonValid } from '@/utils/json-avro/jsonUtils';
import { describe, expect } from 'vitest';

describe('Json utils', () => {
  const object = {
    key1: 'value1',
    key2: {
      key21: 'value21',
      key22: {
        key221: 'value221',
        key222: 'value222',
      },
    },
    array: [
      {
        keyA: 'valueA',
        keyB: 'valueB',
      },
      {
        keyC: {
          keyC1: 'valueC1',
        },
      },
    ],
  };

  it('deepSearch() - should find root object', () => {
    const expectedObject = object;

    const foundObject = deepSearch(object, 'key1', 'value1');

    expect(foundObject).toEqual(expectedObject);
  });

  it('deepSearch() - should find nested object', () => {
    const expectedObject = {
      key221: 'value221',
      key222: 'value222',
    };

    const foundObject = deepSearch(object, 'key222', 'value222');

    expect(foundObject).toEqual(expectedObject);
  });

  it('deepSearch() - should find object in array', () => {
    const expectedObject = {
      keyA: 'valueA',
      keyB: 'valueB',
    };

    const foundObject = deepSearch(object, 'keyA', 'valueA');

    expect(foundObject).toEqual(expectedObject);
  });

  it('deepSearch() - should return null when key with value not found', () => {
    const expectedObject = null;

    const foundObject = deepSearch(object, 'keyX', 'valueX');

    expect(foundObject).toEqual(expectedObject);
  });

  it('isJsonValid() - return true when string is valid json object', () => {
    const testJsonString = JSON.stringify(object);

    const result = isJsonValid(testJsonString);

    expect(result).toEqual(true);
  });

  it('isJsonValid() - return false when string is null', () => {
    const testJsonString = null;

    const result = isJsonValid(testJsonString);

    expect(result).toEqual(false);
  });

  it('isJsonValid() - return false when string invalid', () => {
    const testJsonString = 'incorrect json string';

    const result = isJsonValid(testJsonString);

    expect(result).toEqual(false);
  });
});
