import { PeopleStock } from './PeopleStock';

export interface PeopleStockResponse {
  statusCode: number;
  status: string;
  message: string;
  timeStamp: string;
  errors: any;
  data: PeopleStock[];
}
