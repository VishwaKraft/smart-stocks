import { StockOrder } from './StockOrder';

export interface StockOrderResponse {
  statusCode: number;
  status: string;
  message: string;
  timeStamp: string;
  errors: any;
  data: StockOrder[];
}
