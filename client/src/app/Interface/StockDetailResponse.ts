import { StockDetail } from './StockDetail';

export interface StockDetailResponse {
  statusCode: number;
  status: string;
  message: string;
  timeStamp: string;
  errors: any;
  data: StockDetail;
}
