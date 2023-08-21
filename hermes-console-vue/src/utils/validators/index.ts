export type FieldValidator<TValue> = (v: TValue) => boolean | string;

const validate = (result: boolean | string, errorMsg: string) =>
  result || errorMsg;

export const minLength =
  (length: number, errorMsg: string): FieldValidator<string> =>
  (v: string) =>
    validate(v.length >= length, errorMsg);
export const maxLength =
  (length: number, errorMsg: string): FieldValidator<string> =>
  (v: string) =>
    validate(v.length <= length, errorMsg);
export const matchRegex =
  (pattern: RegExp, errorMsg: string): FieldValidator<string> =>
  (v: string) =>
    validate(pattern.test(v), errorMsg);
export const required =
  (errorMsg: string = 'Required'): FieldValidator<any> =>
  (v: any) =>
    validate(typeof v === 'string' ? !!v.trim() : v != null, errorMsg);

export const min =
  (
    minValue: number,
    errorMsg: string = `Should be grater or equal to ${minValue}`,
  ): FieldValidator<number> =>
  (v: number) =>
    validate(v >= minValue, errorMsg);

export const max =
  (
    maxValue: number,
    errorMsg: string = `Should be lower or equal to ${maxValue}`,
  ): FieldValidator<number> =>
  (v: number) =>
    validate(v <= maxValue, errorMsg);
