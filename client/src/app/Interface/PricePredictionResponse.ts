import { PricePrediction } from './PricePrediction';

export interface PricePredictionResponse {
  statusCode: number;
  status: string;
  message: string;
  timeStamp: string;
  errors: any;
  data: PricePrediction[];
}
