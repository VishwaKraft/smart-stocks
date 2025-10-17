import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { PricePrediction } from 'src/app/Interface/PricePrediction';
import { StockDetailsService } from 'src/app/services/stock-details.service';

@Component({
  selector: 'app-predicted-price',
  templateUrl: './predicted-price.component.html',
  styleUrls: ['./predicted-price.component.css'],
})
export class PredictedPriceComponent implements OnInit {
  requireLoader: boolean = true;

  symbol: string = '';
  predictPrice: PricePrediction[];
  day1: { Date: string; Price: string } = { Date: '', Price: '' };
  day2: { Date: string; Price: string } = { Date: '', Price: '' };
  day3: { Date: string; Price: string } = { Date: '', Price: '' };
  day5: { Date: string; Price: string } = { Date: '', Price: '' };
  day10: { Date: string; Price: string } = { Date: '', Price: '' };
  day20: { Date: string; Price: string } = { Date: '', Price: '' };
  constructor(
    private stockDetailsService: StockDetailsService,
    private activatedRoute: ActivatedRoute,
  ) {}
  ngOnInit(): void {
    this.symbol = this.activatedRoute.snapshot.params['symbol'];
    this.stockDetailsService.fetchPredictiedPrice(this.symbol).subscribe((res) => {
      this.predictPrice = res.data;
      for (var i = 0; i < this.predictPrice.length; i++) {
        this.predictPrice[i].Date = new Date(this.predictPrice[i].Date);
        this.predictPrice[i].Date =
          this.predictPrice[i].Date.getDate() +
          '/' +
          (this.predictPrice[i].Date.getMonth() + 1) +
          '/' +
          this.predictPrice[i].Date.getFullYear();
        this.predictPrice[i].To = parseFloat(this.predictPrice[i].To).toFixed(2).toString();
      }

      this.day1.Date = this.predictPrice[0].Date;
      this.day1.Price = this.predictPrice[0].To;
      this.day2.Date = this.predictPrice[1].Date;
      this.day2.Price = this.predictPrice[1].To;
      this.day3.Date = this.predictPrice[2].Date;
      this.day3.Price = this.predictPrice[2].To;
      this.day5.Date = this.predictPrice[4].Date;
      this.day5.Price = this.predictPrice[4].To;
      this.day10.Date = this.predictPrice[9].Date;
      this.day10.Price = this.predictPrice[9].To;
      this.day20.Date = this.predictPrice[19].Date;
      this.day20.Price = this.predictPrice[19].To;
      this.requireLoader = false;
    });
  }
}
