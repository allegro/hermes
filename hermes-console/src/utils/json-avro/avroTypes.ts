export const recordType = ['record'];
export const iterableTypes = ['map', 'array'];
export const primitiveTypes = [
  'null',
  'int',
  'long',
  'float',
  'double',
  'bytes',
  'string',
  'boolean',
  'enum',
];
export const knownTypes = [...primitiveTypes, ...recordType, ...iterableTypes];

export type AvroObject = any;

export const propertyExists = (object: AvroObject, property: string): boolean =>
  Object.prototype.hasOwnProperty.call(object, property);

export const isValidAvroField = (object: AvroObject): boolean =>
  object == null ||
  !propertyExists(object, 'type') ||
  !propertyExists(object, 'name') ||
  object['name'] === '__metadata';

export function isRootRecord(object: any) {
  return object.type === 'record';
}

export function isNonNullableRecord(object: any) {
  return object.type.type === 'record';
}

export function isNonNullableArrayRecord(object: any) {
  return (
    object.type.type === 'array' && Array.isArray(object.type.items.fields)
  );
}

export function isNullableRecord(object: any) {
  return (
    Array.isArray(object.type) &&
    object.type.find(
      (t: AvroObject) => t && propertyExists(t, 'type') && t.type === 'record',
    )
  );
}

export function isNullablePrimitive(object: any) {
  return (
    Array.isArray(object.type) &&
    object.type.every((v: any) => primitiveTypes.includes(v))
  );
}

export function isNonNullableArrayPrimitive(object: any) {
  return (
    !Array.isArray(object.type) &&
    object.type.type === 'array' &&
    primitiveTypes.includes(object.type.items)
  );
}

export function isNullableArrayComplex(object: any) {
  return (
    Array.isArray(object.type) &&
    object.type.find((t: any) => t !== 'null').type === 'array' &&
    recordType.includes(object.type.find((t: any) => t !== 'null').items.type)
  );
}

export function isNullableArrayPrimitiveItems(object: any) {
  return (
    Array.isArray(object.type) &&
    object.type.find((t: any) => t !== 'null').type === 'array' &&
    primitiveTypes.includes(
      object.type.find((t: any) => t !== 'null').items.type,
    )
  );
}

export function isNullableArrayPrimitiveSimple(object: any) {
  return (
    Array.isArray(object.type) &&
    object.type.find((t: any) => t !== 'null').type === 'array' &&
    primitiveTypes.includes(object.type.find((t: any) => t !== 'null').items)
  );
}

export function isNonNullableCustomType(object: any) {
  return (
    !Array.isArray(object.type) &&
    object.type.type != null &&
    !knownTypes.includes(object.type.type)
  );
}

export function isNullableCustomType(object: any) {
  return (
    Array.isArray(object.type) &&
    !knownTypes.includes(object.type.find((t: any) => t != 'null'))
  );
}

export function isNonNullablePrimitive(object: any) {
  return (
    primitiveTypes.includes(object.type) ||
    (object.type.type != null && primitiveTypes.includes(object.type.type))
  );
}
