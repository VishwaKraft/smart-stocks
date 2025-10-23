import { StockTrend } from './StockTrend';

export interface StockTrendResponse {
  statusCode: number;
  status: string;
  message: string;
  timeStamp: string;
  errors: any;
  data: StockTrend[];
}
