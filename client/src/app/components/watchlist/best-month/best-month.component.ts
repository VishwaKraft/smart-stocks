import { Component, OnInit } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { Router } from '@angular/router';
import { GainerTable } from 'src/app/Interface/GainerTable';
import { GainerLooserService } from 'src/app/services/gainer-looser.service';

@Component({
  selector: 'app-best-month',
  templateUrl: './best-month.component.html',
  styleUrls: ['./best-month.component.css']
})
export class BestMonthComponent implements OnInit {

  title = "Best Stocks to Buy this Month"
  requireLoader: boolean = true;
  monthData: GainerTable[] = [];
  monthTable: MatTableDataSource<GainerTable>;
  displayedColumns: string[] = ['companyName', 'ltp', 'dayChange', 'dayChangePerc'];

  constructor(private router: Router,
    private gainerLooserService: GainerLooserService) { }

  ngOnInit(): void {
    window.scrollTo(0, 0);
    this.gainerLooserService.getTopInXdays(15, 'TOP_LOSER').subscribe(result => {
      console.log(result.data)
      result.data.forEach(x => {
        this.monthData.push({
          companyName: x.companyName,
          dayChange: x.overalLChange,
          dayChangePerc: x.overallChangePerc,
          ltp: x.highPriceRange,
          symbol: x.symbol
        })
      })
      this.requireLoader = false;
      this.monthTable = new MatTableDataSource(this.monthData);
    })
  }

  clickedRecommendation(row: any) {
    let cleanSymbol = row.symbol;
    if (cleanSymbol.endsWith('.NS')) {
      cleanSymbol = cleanSymbol.substring(0, cleanSymbol.length - 3);
    }
    this.router.navigate(['/stockDetails/' + cleanSymbol]);
  }

}
