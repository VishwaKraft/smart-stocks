import { Component, OnInit } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { Router } from '@angular/router';
import { GainerTable } from 'src/app/Interface/GainerTable';
import { GainerLooserService } from 'src/app/services/gainer-looser.service';
import { NGXLogger } from 'ngx-logger';

@Component({
  selector: 'app-best-month',
  templateUrl: './best-month.component.html',
  styleUrls: ['./best-month.component.css'],
})
export class BestMonthComponent implements OnInit {
  title = 'Best Stocks to Buy this Month';
  requireLoader: boolean = true;
  monthData: GainerTable[] = [];
  monthTable: MatTableDataSource<GainerTable>;
  displayedColumns: string[] = ['companyName', 'ltp', 'dayChange', 'dayChangePerc'];

  constructor(
    private router: Router,
    private gainerLooserService: GainerLooserService,
    private logger: NGXLogger,
  ) {}

  ngOnInit(): void {
    window.scrollTo(0, 0);
    this.gainerLooserService.getTopInXdays(15, 'TOP_LOSER').subscribe((result) => {
      this.logger.info(result.data);
      result.data.forEach((x) => {
        this.monthData.push({
          companyName: x.companyName,
          dayChange: x.overalLChange,
          dayChangePerc: x.overallChangePerc,
          ltp: x.highPriceRange,
          symbol: x.symbol,
        });
      });
      this.requireLoader = false;
      this.monthTable = new MatTableDataSource(this.monthData);
    });
  }

  clickedRecommendation(row: any) {
    console.log(row.symbol);
    this.router.navigate(['/stockDetails/' + row.symbol]);
  }
}
