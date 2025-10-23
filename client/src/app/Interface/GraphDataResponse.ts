import { GraphData } from './GraphData';

export interface GraphDataResponse {
  statusCode: number;
  status: string;
  message: string;
  timeStamp: string;
  errors: any;
  data: GraphData[];
}
