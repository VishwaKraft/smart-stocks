import { NewsBody } from './NewsBody';

export interface NewsResponse {
  statusCode: number;
  status: string;
  message: string;
  timeStamp: string;
  errors: any;
  data: NewsBody[];
}
