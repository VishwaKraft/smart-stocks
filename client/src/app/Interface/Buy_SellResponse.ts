import { Buy_Sell } from './Buy_Sell';

export interface Buy_SellResponse {
  statusCode: number;
  status: string;
  message: string;
  timeStamp: string;
  errors: any;
  data: Buy_Sell[];
}
