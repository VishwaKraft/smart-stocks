export class responseModel {
  constructor(
    public statusCode: number,
    public status: string,
    public message: string,
    public timeStamp: string,
    public errors: string,
    public data: any,
  ) {}
}
