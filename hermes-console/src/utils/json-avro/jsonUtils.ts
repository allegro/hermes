import { propertyExists } from '@/utils/json-avro/avroTypes';

export const isJsonValid = (text?: string | null): boolean => {
  if (!text) return false;
  try {
    return !!JSON.parse(text);
  } catch (_) {
    return false;
  }
};

export const deepSearch = (object: any, key: string, value: string): any => {
  if (propertyExists(object, key) && object[key] === value) return object;
  for (let i = 0; i < Object.keys(object).length; i++) {
    const v = object[Object.keys(object)[i]];
    if (typeof v === 'object' && v != null) {
      const o = deepSearch(object[Object.keys(object)[i]], key, value);
      if (o != null) return o;
    }
  }
  return null;
};
