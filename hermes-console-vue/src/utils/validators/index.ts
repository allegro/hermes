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
    validate(!!v, errorMsg);
