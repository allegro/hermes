import {
  type AvroObject,
  isNonNullableArrayPrimitive,
  isNonNullableArrayRecord,
  isNonNullableCustomType,
  isNonNullablePrimitive,
  isNonNullableRecord,
  isNullableArrayComplex,
  isNullableArrayPrimitiveItems,
  isNullableArrayPrimitiveSimple,
  isNullableCustomType,
  isNullablePrimitive,
  isNullableRecord,
  isRootRecord,
  isValidAvroField,
  propertyExists,
} from '@/utils/json-avro/avroTypes';
import { deepSearch, isJsonValid } from '@/utils/json-avro/jsonUtils';

export const getAvroPaths = (schema?: string): string[] => {
  let avroPaths: string[] = [];
  try {
    if (isJsonValid(schema)) {
      const parsedSchema = JSON.parse(schema!);
      avroPaths = flattenAvro(parsedSchema);
    }
  } catch (_) {
    console.log('Cannot parse schema and get avro paths.');
  }
  return avroPaths;
};

export function flattenAvro(
  object: AvroObject,
  prefix: string = '',
  fullObject = object,
): string[] {
  if (isValidAvroField(object)) return [];
  return Object.keys(object).reduce((acc: any, key) => {
    if (key !== 'name') return acc;

    if (isRootRecord(object)) {
      object.fields.forEach((field: AvroObject) => {
        const path = getCurrentPath(prefix, object, false, true);
        return (acc = acc.concat(flattenAvro(field, path, fullObject)));
      });
      return acc;
    }

    if (isNonNullableRecord(object)) {
      acc.push(getCurrentPath(prefix, object));
      object.type.fields.forEach((field: AvroObject) => {
        const path = getCurrentPath(prefix, object, false, false);
        return (acc = acc.concat(flattenAvro(field, path, fullObject)));
      });
      return acc;
    }

    if (isNonNullableArrayRecord(object)) {
      acc.push(getCurrentPath(prefix, object, true));
      object.type.items.fields.forEach((field: AvroObject) => {
        const path = getCurrentPath(prefix, object, true, false);
        return (acc = acc.concat(flattenAvro(field, path, fullObject)));
      });
      return acc;
    }

    if (isNullableRecord(object)) {
      acc.push(getCurrentPath(prefix, object));
      object.type
        .find((t: AvroObject) => t.type === 'record')
        .fields.forEach((field: AvroObject) => {
          return (acc = acc.concat(
            flattenAvro(field, getCurrentPath(prefix, object), fullObject),
          ));
        });
      return acc;
    }

    if (isNonNullableArrayPrimitive(object)) {
      acc.push(getCurrentPath(prefix, object, true));
      return acc;
    }

    if (isNullableArrayComplex(object)) {
      acc.push(getCurrentPath(prefix, object, true));
      object.type
        .find((t: any) => t !== 'null')
        .items.fields.forEach((field: AvroObject) => {
          const path = getCurrentPath(prefix, object, true, false);
          return (acc = acc.concat(flattenAvro(field, path, fullObject)));
        });
      return acc;
    }

    if (isNullableArrayPrimitiveItems(object)) {
      acc.push(getCurrentPath(prefix, object, true, false));
      return acc;
    }

    if (isNullableArrayPrimitiveSimple(object)) {
      acc.push(getCurrentPath(prefix, object, true));
      return acc;
    }

    if (isNullableCustomType(object)) {
      const customTypeName = object.type.find((t: any) => t != 'null');
      const foundType = deepSearch(fullObject, 'name', customTypeName);
      acc = getCustomObjectPaths(acc, prefix, object, foundType, fullObject);
      return acc;
    }

    if (isNonNullableCustomType(object)) {
      const foundType = deepSearch(fullObject, 'name', object.type.type);
      acc = getCustomObjectPaths(acc, prefix, object, foundType, fullObject);
      return acc;
    }

    if (isNullablePrimitive(object) || isNonNullablePrimitive(object)) {
      acc.push(getCurrentPath(prefix, object));
      return acc;
    }
  }, []);
}

const getCurrentPath = (
  prefix: string,
  object: AvroObject,
  isArray: boolean = false,
  isRoot: boolean = false,
): string =>
  (isRoot ? '' : prefix + '.' + object['name']) + (isArray ? '[*]' : '');

const getCustomObjectPaths = (
  acc: any,
  prefix: string,
  object: any,
  foundType: AvroObject,
  fullObject: AvroObject,
) => {
  acc.push(getCurrentPath(prefix, object, false, false));
  if (foundType && propertyExists(foundType, 'fields')) {
    foundType.fields.forEach((field: AvroObject) => {
      const path = getCurrentPath(prefix, object, false, false);
      return (acc = acc.concat(flattenAvro(field, path, fullObject)));
    });
  }
  return acc;
};
