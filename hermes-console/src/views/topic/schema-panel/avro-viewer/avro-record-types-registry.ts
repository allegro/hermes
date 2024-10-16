import type {
  AvroSchema,
  NameOrType,
  RecordType,
  Type,
} from '@/views/topic/schema-panel/AvroTypes';

interface WorkingRecord {
  record: RecordType;
  namespace?: string;
  justName: string;
}

const namespace = (
  { name, namespace }: AvroSchema,
  parentNamespace?: string,
): string | undefined => {
  if (name.includes('.')) {
    const namespaceNameSeparatorIndex = name.lastIndexOf('.');
    return name.substring(0, namespaceNameSeparatorIndex);
  }
  if (namespace) {
    return namespace;
  }
  return parentNamespace;
};

const justRecordName = ({ name }: AvroSchema): string => {
  if (name.includes('.')) {
    const namespaceNameSeparatorIndex = name.lastIndexOf('.');
    return name.substring(namespaceNameSeparatorIndex + 1);
  }
  return name;
};

const workingRecord = (
  record: RecordType,
  parentNamespace?: string,
): WorkingRecord => {
  return {
    record,
    namespace: namespace(record, parentNamespace),
    justName: justRecordName(record),
  };
};

const getTypesList = (type: Type): NameOrType[] =>
  Array.isArray(type) ? type : [type];

const getDirectSubRecords = (parent: WorkingRecord): WorkingRecord[] => {
  return parent.record.fields
    .flatMap((field) => getTypesList(field.type))
    .filter((field) => field.type === 'record')
    .map((record) => workingRecord(record, parent.namespace));
};

const getAllSubRecords = (
  unprocessedRecords: WorkingRecord[],
  processedRecords: WorkingRecord[],
): WorkingRecord[] => {
  if (unprocessedRecords.length === 0) {
    return processedRecords;
  }
  const currentLevelSubRecords =
    unprocessedRecords.flatMap(getDirectSubRecords);
  return getAllSubRecords(currentLevelSubRecords, [
    ...processedRecords,
    ...currentLevelSubRecords,
  ]);
};

const validQualifiers = (
  { namespace, justName }: WorkingRecord,
  rootNamespace?: string,
): string[] => {
  if (!namespace) {
    return [justName];
  }
  if (namespace === rootNamespace) {
    return [justName, `${namespace}.${justName}`];
  }
  return [`${namespace}.${justName}`];
};

const associateByValidQualifiers = (
  records: WorkingRecord[],
  rootNamespace?: string,
): Map<string, RecordType> => {
  return new Map(
    records.flatMap((record) => {
      return validQualifiers(record, rootNamespace).map((qualifier) => [
        qualifier,
        record.record,
      ]);
    }),
  );
};

export const createRecordTypesRegistry = (
  schema: AvroSchema,
): Map<string, RecordType> => {
  const workingRootRecord = workingRecord({
    ...schema.type,
    name: schema.name,
    namespace: schema.namespace,
  });
  const allRecords = getAllSubRecords([workingRootRecord], []);
  return associateByValidQualifiers(allRecords, workingRootRecord.namespace);
};

export const getRecordTypesRegistryEntry = (
  registry: Map<string, RecordType>,
  referenceType: Type,
): RecordType | undefined => {
  return getTypesList(referenceType)
    .filter((type) => typeof type == 'string')
    .map((typeName) => registry.get(typeName))
    .find((record) => !!record);
};
