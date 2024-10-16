export interface AvroSchema {
  type: Type;
  name: string;
  namespace?: string;
  doc: string;
  fields?: Field[];
  default?: DefaultTypes;
}

export type TypeNames = 'record' | 'array' | 'null' | 'map' | string;

export interface Field {
  name: string;
  type: Type;
  default?: DefaultTypes;
  fields: Field[];
}

export interface RecordType extends AvroSchema {
  type: 'record';
  name: string;
  namespace: string;
  fields: Field[];
}

export interface ArrayType extends AvroSchema {
  type: 'array';
  items: Type;
}

export interface MapType extends AvroSchema {
  type: 'map';
  values: Type;
}

export interface EnumType extends AvroSchema {
  type: 'enum';
  name: string;
  symbols: string[];
}

export interface NamedType extends AvroSchema {
  type: string;
}

export interface LogicalType extends AvroSchema {
  type: string;
  logicalType: string;
}

export type Type = NameOrType | NameOrType[];

export type DefaultTypes = string | number | null | boolean | any[];

export type NameOrType =
  | TypeNames
  | RecordType
  | ArrayType
  | NamedType
  | LogicalType
  | MapType
  | EnumType
  | any;
