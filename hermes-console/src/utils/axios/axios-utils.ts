import type { AxiosResponse } from 'axios';

export type ResponsePromise<T> = Promise<AxiosResponse<T>>;
