import { PaymentHistory } from './PaymentHistory';

export interface PaymentHistoryResponse {
  statusCode: number;
  status: string;
  message: string;
  timeStamp: string;
  errors: any;
  data: PaymentHistory[];
}
