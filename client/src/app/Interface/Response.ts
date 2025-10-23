import { SearchResponse } from './SearchResponse';

export interface Response {
  statusCode: number;
  status: string;
  message: string;
  timeStamp: string;
  errors: any;
  data: SearchResponse[];
}
