import { describe, expect } from 'vitest';
import { flattenAvro } from '@/utils/json-avro/jsonAvroUtils';

describe('Json-Avro tools', () => {
  it('should find all avro path from avro json example schema', () => {
    const jsonAvroSchemaObject = {
      namespace: 'enterprise.thing',
      type: 'record',
      name: 'rating_event',
      fields: [
        { name: 'type', type: 'string' },
        {
          name: 'account',
          type: {
            type: 'record',
            name: 'account',
            fields: [
              { name: 'account_id', type: 'string' },
              { name: 'id', type: 'string' },
            ],
          },
        },
        {
          name: 'rating',
          type: {
            type: 'record',
            name: 'rating',
            fields: [
              { name: 'rating_type', type: 'string' },
              {
                name: 'rating_results',
                type: {
                  type: 'array',
                  items: {
                    type: 'record',
                    name: 'rating_results',
                    fields: [
                      {
                        name: 'rating_result',
                        type: {
                          type: 'record',
                          name: 'rating_result',
                          fields: [
                            { name: 'name', type: ['null', 'string'] },
                            { name: 'age', type: 'float' },
                            { name: 'city_code', type: ['null', 'float'] },
                          ],
                        },
                      },
                    ],
                  },
                },
              },
              {
                name: 'related_to',
                type: [
                  'null',
                  {
                    type: 'record',
                    name: 'related_to',
                    fields: [{ name: 'category', type: 'string' }],
                  },
                ],
              },
            ],
          },
        },
      ],
    };

    const foundPaths = flattenAvro(jsonAvroSchemaObject);

    const expectedPaths: string[] = [
      '.type',
      '.account',
      '.account.account_id',
      '.account.id',
      '.rating',
      '.rating.rating_type',
      '.rating.rating_results[*]',
      '.rating.rating_results[*].rating_result',
      '.rating.rating_results[*].rating_result.name',
      '.rating.rating_results[*].rating_result.age',
      '.rating.rating_results[*].rating_result.city_code',
      '.rating.related_to',
      '.rating.related_to.category',
    ];
    expect(foundPaths).toEqual(expectedPaths);
  });

  it('should return empty paths array when object structure is not valid avro scheme', () => {
    const incorrectAvroSchemaObject = {
      keyA: 'valueA',
      name: 'Incorrect object',
    };

    const foundPaths = flattenAvro(incorrectAvroSchemaObject);

    const expectedPaths: string[] = [];
    expect(foundPaths).toEqual(expectedPaths);
  });

  it('should return empty paths array when object is null', () => {
    const incorrectAvroSchemaObject = null;

    const foundPaths = flattenAvro(incorrectAvroSchemaObject);

    const expectedPaths: string[] = [];
    expect(foundPaths).toEqual(expectedPaths);
  });
});
