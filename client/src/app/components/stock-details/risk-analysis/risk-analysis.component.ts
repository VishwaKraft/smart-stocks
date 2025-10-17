import { Component, OnInit } from '@angular/core';
import { StockDetailsService } from 'src/app/services/stock-details.service';
import { ActivatedRoute } from '@angular/router';
import { stockTrendModel } from 'src/app/models/stockTrendModel';
import { Chart } from 'chart.js';

@Component({
  selector: 'app-risk-analysis',
  templateUrl: './risk-analysis.component.html',
  styleUrls: ['./risk-analysis.component.css'],
})
export class RiskAnalysisComponent implements OnInit {
  stockDetailsService: StockDetailsService;
  stockDetails: any;

  activatedRoute: ActivatedRoute;
  symbol = '';
  stockTrend: stockTrendModel[];
  strongBuy: number = 0;
  buy: number = 0;
  hold: number = 0;
  sell: number = 0;
  strongSell: number = 0;
  chart: any;
  hide: boolean = false;
  oppHide = !this.hide;
  constructor(stockDetailsService: StockDetailsService, activatedRoute: ActivatedRoute) {
    this.stockDetailsService = stockDetailsService;
    this.activatedRoute = activatedRoute;
  }
  ngOnInit(): void {
    this.symbol = this.activatedRoute.snapshot.params['symbol'];
    this.stockDetailsService.fetchStockTrend(this.symbol).subscribe((res) => {
      this.stockTrend = res.data;
      for (var i = 0; i < this.stockTrend.length; i++) {
        this.strongBuy += this.stockTrend[i].strongBuy;
        this.strongBuy += this.stockTrend[i].buy;
        this.hold += this.stockTrend[i].hold;
        this.sell += this.stockTrend[i].sell;
        this.strongSell += this.stockTrend[i].strongSell;
      }

      if (
        this.strongBuy == 0 &&
        this.buy == 0 &&
        this.hold == 0 &&
        this.sell == 0 &&
        this.strongSell == 0
      ) {
        this.hide = true;
        this.oppHide = !this.hide;
      }
      this.chart = new Chart('canvas', {
        type: 'doughnut',
        data: {
          labels: ['Strong Buy', 'Buy', 'Hold', 'Sell', 'Strong Sell'],
          datasets: [
            {
              data: [this.strongBuy, this.buy, this.hold, this.sell, this.strongSell],
              backgroundColor: [
                'rgba(0, 100, 0, 0.8)',
                'rgba(0, 128, 0, 0.6)',
                'rgba(255, 191, 0, 0.8)',
                'rgba(255, 127,127, 0.8)',
                'rgba(255, 0, 0, 0.8)',
              ],
              fill: false,
            },
          ],
        },
        options: {
          legend: {
            display: true,
          },
          tooltips: {
            enabled: true,
          },
        },
      });
    });
  }
}
