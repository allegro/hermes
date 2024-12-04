import {
  createRecordTypesRegistry,
  getRecordTypesRegistryEntry,
} from '@/views/topic/schema-panel/avro-viewer/avro-record-types-registry';
import { describe, expect } from 'vitest';
import type {
  AvroSchema,
  Field,
  RecordType,
} from '@/views/topic/schema-panel/AvroTypes';

describe('avro records registry', () => {
  const createSchema = (
    fields: any,
    name: string = 'Test',
    namespace?: string,
  ): AvroSchema => ({
    name,
    namespace,
    fields: fields,
    type: {
      type: 'record',
      fields: fields,
    },
    doc: '',
  });

  const createStringField = (name: string): Field => ({
    name,
    type: 'string',
    fields: [],
  });

  const createRecordField = (name: string, type: RecordType): Field => ({
    name,
    type: ['null', type],
    fields: [],
  });

  const createRecordType = (
    name: string,
    namespace: string | undefined = undefined,
    fields: Field[] = [createStringField('x')],
  ): RecordType => ({
    type: 'record',
    name,
    namespace: namespace as string,
    fields,
    doc: '',
  });

  it('should handle no sub records', () => {
    // given
    const schema = createSchema([
      createStringField('field1'),
      createStringField('field2'),
    ]);

    // when
    const result = createRecordTypesRegistry(schema);

    // then
    const expectedResult = new Set([]);
    expect(new Set(result.entries())).toEqual(expectedResult);
  });

  it('should handle sub record with namespace for root with namespace', () => {
    // given
    const subRecord = createRecordType('SubRecord', 'com.example2');
    const schema = createSchema(
      [createRecordField('field1', subRecord)],
      'Test',
      'com.example',
    );

    // when
    const result = createRecordTypesRegistry(schema);

    // then
    const expectedResult = new Set([['com.example2.SubRecord', subRecord]]);
    expect(new Set(result.entries())).toEqual(expectedResult);
  });

  it('should handle sub record with namespace for root without namespace', () => {
    // given
    const subRecord = createRecordType('SubRecord', 'com.example2');
    const schema = createSchema([createRecordField('field1', subRecord)]);

    // when
    const result = createRecordTypesRegistry(schema);

    // then
    const expectedResult = new Set([['com.example2.SubRecord', subRecord]]);
    expect(new Set(result.entries())).toEqual(expectedResult);
  });

  it('should handle sub record without namespace for root with namespace', () => {
    // given
    const subRecord = createRecordType('SubRecord');
    const schema = createSchema(
      [createRecordField('field1', subRecord)],
      'Test',
      'com.example',
    );

    // when
    const result = createRecordTypesRegistry(schema);

    // then
    const expectedResult = new Set([
      ['com.example.SubRecord', subRecord],
      ['SubRecord', subRecord],
    ]);
    expect(new Set(result.entries())).toEqual(expectedResult);
  });

  it('should handle sub record without namespace for root without namespace', () => {
    // given
    const subRecord = createRecordType('SubRecord');
    const schema = createSchema([createRecordField('field1', subRecord)]);

    // when
    const result = createRecordTypesRegistry(schema);

    // then
    const expectedResult = new Set([['SubRecord', subRecord]]);
    expect(new Set(result.entries())).toEqual(expectedResult);
  });

  it('should handle sub record with namespace in name', () => {
    // given
    const subRecord = createRecordType('com.example2.SubRecord');
    const schema = createSchema(
      [createRecordField('field1', subRecord)],
      'Test',
      'com.example',
    );

    // when
    const result = createRecordTypesRegistry(schema);

    // then
    const expectedResult = new Set([['com.example2.SubRecord', subRecord]]);
    expect(new Set(result.entries())).toEqual(expectedResult);
  });

  it('should handle sub record without namespace for root with namespace in name', () => {
    // given
    const subRecord = createRecordType('SubRecord');
    const schema = createSchema(
      [createRecordField('field1', subRecord)],
      'com.example.Test',
    );

    // when
    const result = createRecordTypesRegistry(schema);

    // then
    const expectedResult = new Set([
      ['com.example.SubRecord', subRecord],
      ['SubRecord', subRecord],
    ]);
    expect(new Set(result.entries())).toEqual(expectedResult);
  });

  it('should handle sub record 2 levels down without namespace inherited from parent', () => {
    // given
    const subSubRecord = createRecordType('SubSubRecord');
    const subRecord = createRecordType('SubRecord', 'com.example2', [
      createRecordField('field2', subSubRecord),
    ]);
    const schema = createSchema([createRecordField('field1', subRecord)]);

    // when
    const result = createRecordTypesRegistry(schema);

    // then
    const expectedResult = new Set([
      ['com.example2.SubRecord', subRecord],
      ['com.example2.SubSubRecord', subSubRecord],
    ]);
    expect(new Set(result.entries())).toEqual(expectedResult);
  });

  it('should handle sub record 2 levels down without namespace inherited from parent name', () => {
    // given
    const subSubRecord = createRecordType('SubSubRecord');
    const subRecord = createRecordType('com.example2.SubRecord', undefined, [
      createRecordField('field2', subSubRecord),
    ]);
    const schema = createSchema([createRecordField('field1', subRecord)]);

    // when
    const result = createRecordTypesRegistry(schema);

    // then
    const expectedResult = new Set([
      ['com.example2.SubRecord', subRecord],
      ['com.example2.SubSubRecord', subSubRecord],
    ]);
    expect(new Set(result.entries())).toEqual(expectedResult);
  });

  it('should handle sub record 2 levels down without namespace inherited from root', () => {
    // given
    const subSubRecord = createRecordType('SubSubRecord');
    const subRecord = createRecordType('SubRecord', undefined, [
      createRecordField('field2', subSubRecord),
    ]);
    const schema = createSchema(
      [createRecordField('field1', subRecord)],
      'Test',
      'com.example',
    );

    // when
    const result = createRecordTypesRegistry(schema);

    // then
    const expectedResult = new Set([
      ['com.example.SubRecord', subRecord],
      ['SubRecord', subRecord],
      ['com.example.SubSubRecord', subSubRecord],
      ['SubSubRecord', subSubRecord],
    ]);
    expect(new Set(result.entries())).toEqual(expectedResult);
  });

  it('should handle two sub records one with namespace one without', () => {
    // given
    const subRecord1 = createRecordType('SubRecord1', 'com.example2');
    const subRecord2 = createRecordType('SubRecord2');
    const schema = createSchema(
      [
        createRecordField('field1', subRecord1),
        createRecordField('field2', subRecord2),
      ],
      'Test',
      'com.example',
    );

    // when
    const result = createRecordTypesRegistry(schema);

    // then
    const expectedResult = new Set([
      ['com.example2.SubRecord1', subRecord1],
      ['com.example.SubRecord2', subRecord2],
      ['SubRecord2', subRecord2],
    ]);
    expect(new Set(result.entries())).toEqual(expectedResult);
  });

  it('should find record reference when present', () => {
    // given
    const record = createRecordType('com.example.SubRecord');
    const registry = new Map([
      ['com.example.SubRecord', record],
      ['SubRecord', record],
    ]);

    // and
    const referenceType = 'SubRecord';

    // when
    const result = getRecordTypesRegistryEntry(registry, referenceType);

    // then
    expect(result).toEqual(record);
  });

  it('should find record by fully qualified reference when present', () => {
    // given
    const record = createRecordType('com.example2.SubRecord');
    const registry = new Map([['com.example2.SubRecord', record]]);

    // and
    const referenceType = 'com.example2.SubRecord';

    // when
    const result = getRecordTypesRegistryEntry(registry, referenceType);

    // then
    expect(result).toEqual(record);
  });

  it('should find record by reference when null or reference type', () => {
    // given
    const record = createRecordType('com.example.SubRecord');
    const registry = new Map([
      ['com.example.SubRecord', record],
      ['SubRecord', record],
    ]);

    // and
    const referenceType = ['SubRecord', 'null'];

    // when
    const result = getRecordTypesRegistryEntry(registry, referenceType);

    // then
    expect(result).toEqual(record);
  });

  it('should not find record by reference when absent', () => {
    // given
    const record = createRecordType('com.example.SubRecord');
    const registry = new Map([
      ['com.example.SubRecord', record],
      ['SubRecord', record],
    ]);

    // and
    const referenceType = 'com.example2.SubRecord';

    // when
    const result = getRecordTypesRegistryEntry(registry, referenceType);

    // then
    expect(result).toBeUndefined();
  });

  it('should not find record when not reference type', () => {
    // given
    const record = createRecordType('com.example.SubRecord');
    const registry = new Map([
      ['com.example.SubRecord', record],
      ['SubRecord', record],
    ]);

    // and
    const referenceType = record;

    // when
    const result = getRecordTypesRegistryEntry(registry, referenceType);

    // then
    expect(result).toBeUndefined();
  });
});
