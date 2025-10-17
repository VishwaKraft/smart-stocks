import { Component, OnInit, ViewChild } from '@angular/core';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { GainerTable } from 'src/app/Interface/GainerTable';
import { GainerLooserService } from 'src/app/services/gainer-looser.service';
import { NGXLogger } from 'ngx-logger';

@Component({
  selector: 'app-top-gainer',
  templateUrl: './top-gainer.component.html',
  styleUrls: ['./top-gainer.component.css'],
})
export class TopGainerComponent implements OnInit {
  title = 'Top Gainer';
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
    this.gainerLooserService.getTopGainer().subscribe((value) => {
      value.data.stock.exploreCompanyList.TOP_GAINERS.forEach((x) => {
        this.gainerData.push({
          companyName: x.company.companyName,
          dayChange: x.stats.dayChange,
          dayChangePerc: x.stats.dayChangePerc,
          ltp: x.stats.ltp,
          symbol: x.stats.symbol,
        });
      });
      this.gainerTable = new MatTableDataSource(this.gainerData);
      this.gainerTable.paginator = this.paginator;
      this.gainerTable.sort = this.sort;
      this.requireLoader = false;
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
    var params = 'TOP_GAINER';
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
