import { Component, OnInit, ViewChild } from '@angular/core';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { Router } from '@angular/router';
import { GainerTable } from 'src/app/Interface/GainerTable';
import { GainerLooserService } from 'src/app/services/gainer-looser.service';
import { NGXLogger } from 'ngx-logger';

@Component({
  selector: 'app-top-loser',
  templateUrl: './top-loser.component.html',
  styleUrls: ['./top-loser.component.css'],
})
export class TopLoserComponent implements OnInit {
  title = 'Top Loser';
  userName: number = 5;
  gainerData: GainerTable[] = [];
  gainerTable: MatTableDataSource<GainerTable>;
  displayedColumns: string[] = ['companyName', 'ltp', 'dayChange', 'dayChangePerc'];

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;
  requireLoader: boolean = true;

  constructor(
    private router: Router,
    private gainerLooserService: GainerLooserService,
    private logger: NGXLogger,
  ) {}

  ngOnInit(): void {
    window.scrollTo(0, 0);
    this.gainerLooserService.getTopLooser().subscribe((value) => {
      value.data.stock.exploreCompanyList.TOP_LOSERS.forEach((x) => {
        this.gainerData.push({
          companyName: x.company.companyName,
          dayChange: x.stats.dayChange,
          dayChangePerc: x.stats.dayChangePerc,
          ltp: x.stats.ltp,
          symbol: x.stats.symbol,
        });
      });
      this.requireLoader = false;
      this.gainerTable = new MatTableDataSource(this.gainerData);
      this.gainerTable.paginator = this.paginator;
      this.gainerTable.sort = this.sort;
    });
  }

  clicked(row: any) {
    this.logger.debug(row.symbol.substring(row.symbol.length - 3));
    if (row.symbol.substring(row.symbol.length - 3) != '.NS') {
      this.router.navigate(['/stockDetails/' + row.symbol + '.NS']);
    } else {
      this.router.navigate(['/stockDetails/' + row.symbol]);
    }
  }

  getTopGainerLossers() {
    this.requireLoader = true;
    this.gainerTable = new MatTableDataSource();
    var params = 'TOP_LOSER';
    this.gainerLooserService.getTopInXdays(this.userName, params).subscribe((result) => {
      this.logger.debug(result.data);
      this.gainerData = [];
      result.data.forEach((x) => {
        this.gainerData.push({
          companyName: x.companyName,
          dayChange: x.overalLChange,
          dayChangePerc: x.overallChangePerc,
          ltp: x.highPriceRange,
          symbol: x.symbol,
        });
      });
      this.gainerTable = new MatTableDataSource(this.gainerData);
      this.gainerTable.paginator = this.paginator;
      this.gainerTable.sort = this.sort;
      this.requireLoader = false;
    });
  }
}
