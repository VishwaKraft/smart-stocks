import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { GainerLooserService } from 'src/app/services/gainer-looser.service';

interface StockDisplay {
  ticker: string;
  name: string;
  price: number;
  change: number;
  changePerc: number;
  sector: string;
}

@Component({
  selector: 'app-recommendation',
  templateUrl: './recommendation.component.html',
  styleUrls: ['./recommendation.component.css']
})
export class RecommendationComponent implements OnInit {

  title = "Top Recommended Stocks for Today";
  recommendationData: StockDisplay[] = [];
  requireLoader: boolean = true;

  constructor(private router: Router, private gainerLooserService: GainerLooserService) { }

  ngOnInit(): void {
    window.scrollTo(0, 0);
    this.gainerLooserService.getTopInXdays(1, 'TOP_LOSER').subscribe(v => {
      // Find the first top loser to pass as symbol
      const symbol = v.data && v.data.length > 0 ? v.data[0].symbol : 'TATASTEEL.NS';
      
      this.gainerLooserService.getTopRecommendation(symbol).subscribe({
        next: (value) => {
          if (value && value.data) {
            value.data.forEach((element: any) => {
              this.recommendationData.push({
                ticker: element.symbol,
                name: element.longName || element.symbol,
                price: parseFloat(element.currentPrice?.raw || element.currentPrice?.fmt?.replace(/,/g, '') || '0'),
                change: parseFloat(element.change?.raw || element.change?.fmt?.replace(/,/g, '') || '0'),
                changePerc: parseFloat(element.changePercentage?.raw || element.changePercentage?.fmt?.replace(/[%+,]/g, '') || '0'),
                sector: 'AI Pick'
              });
            });
          }
          this.requireLoader = false;
        },
        error: () => {
          this.requireLoader = false;
        }
      });
    });
  }

  clickedRecommendation(ticker: string) {
    this.router.navigate(['/stockDetails/' + ticker]);
  }

  formatChange(val: number): string {
    return val > 0 ? `+${val.toFixed(2)}` : val.toFixed(2);
  }
}
