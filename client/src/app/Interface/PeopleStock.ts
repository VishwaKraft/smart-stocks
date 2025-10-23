export interface PeopleStock {
  symbol: string;
  longName: string;
  shortName: string;
  currentPrice: { fmt: string };
  previousClose: { fmt: string };
  change: { fmt: string };
  changePercentage: { fmt: string };
}
