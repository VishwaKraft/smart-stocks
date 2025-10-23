import { SellStocks } from './SellStocks';

export interface SellStocksResponse {
  statusCode: number;
  status: string;
  message: string;
  timeStamp: string;
  errors: any;
  data: SellStocks[];
}
