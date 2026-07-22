import { Component, OnInit, ViewChild } from '@angular/core';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { GainerTable } from 'src/app/Interface/GainerTable';
import { GainerLooserService } from 'src/app/services/gainer-looser.service';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-top-gainer',
  templateUrl: './top-gainer.component.html',
  styleUrls: ['./top-gainer.component.css']
})
export class TopGainerComponent implements OnInit {

  title = "Top Gainer"
  userName: number = 5;
  gainerData: GainerTable[] = [];
  gainerTable: MatTableDataSource<GainerTable>;
  displayedColumns: string[] = ['companyName', 'ltp', 'dayChange', 'dayChangePerc'];

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;
  requireLoader: boolean = true;

  constructor(private router: Router, private gainerLooserService: GainerLooserService) { }

  ngOnInit(): void {
    window.scrollTo(0, 0);
    this.getTopGainerLossers();
  }

  clicked(row: any) {
    let cleanSymbol = row.symbol;
    if (cleanSymbol.endsWith('.NS')) {
      cleanSymbol = cleanSymbol.substring(0, cleanSymbol.length - 3);
    }
    this.router.navigate(['/stockDetails/' + cleanSymbol]);
  }

  getTopGainerLossers() {
    this.requireLoader = true;
    this.gainerTable = new MatTableDataSource();
    var params = 'TOP_GAINER';
    this.gainerLooserService.getTopInXdays(this.userName, params)
      .subscribe(result => {
        console.log(result.data);
        this.gainerData = [];
        result.data.forEach(x => {
          this.gainerData.push({
            companyName: x.companyName,
            dayChange: x.overalLChange,
            dayChangePerc: x.overallChangePerc,
            ltp: x.highPriceRange,
            symbol: x.symbol
          })
        })
        this.gainerTable = new MatTableDataSource(this.gainerData);
        this.gainerTable.paginator = this.paginator;
        this.gainerTable.sort = this.sort
        this.requireLoader = false;
      })
  }

}
