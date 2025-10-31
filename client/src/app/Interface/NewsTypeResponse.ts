import { NewsType } from "./NewsType";

export interface NewsTypeResponse {
  statusCode: number;
  status: string;
  message: string;
  timeStamp: string;
  errors: any;
  data: NewsType[];
}
