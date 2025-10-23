import { MockStock } from './MockStock';

export interface MockStockResponse {
  statusCode: number;
  status: string;
  message: string;
  timeStamp: string;
  errors: any;
  data: MockStock[];
}
