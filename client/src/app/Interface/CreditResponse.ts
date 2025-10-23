export interface CreditResponse {
  statusCode: number;
  status: string;
  message: string;
  timeStamp: string;
  errors: any;
  data: { money: number; userProfit: number };
}
