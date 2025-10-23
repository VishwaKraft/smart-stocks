export interface StockDetail {
  shortName: string;
  longName: string;
  exchange: string;
  symbol: string;
  sector: string;
  industry: string;
  currentPrice: { fmt: string };
  change: { fmt: string };
  changePercentage: { fmt: string };
  previousClose: { fmt: string };
  open: { fmt: string };
  dayHigh: { fmt: string };
  dayLow: { fmt: string };
  fiftyTwoWeekLow: { fmt: string };
  fiftyTwoWeekHigh: { fmt: string };
  forwardPE: { fmt: string };
  volume: { fmt: string };
  longBusinessSummary: string;
  marketCap: { fmt: string };
  founded: string;
  enterpriseValue: string;
  priceToBook: { fmt: string };
}
