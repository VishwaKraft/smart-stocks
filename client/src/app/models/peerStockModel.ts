export class peerStockModel {
  constructor(
    public symbol: string,
    public longName: string,
    public shortName: string,
    public currentPrice: { fmt: string },
    public previousClose: { fmt: string },
    public change: { fmt: string },
    public changePercentage: { fmt: string },
  ) {}
}
