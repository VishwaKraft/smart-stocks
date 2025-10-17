import { Component, OnInit, ViewChild } from '@angular/core';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { Router } from '@angular/router';
import { RecommendationFive } from 'src/app/Interface/RecommendationFive';
import { GainerLooserService } from 'src/app/services/gainer-looser.service';

@Component({
  selector: 'app-all-stock',
  templateUrl: './all-stock.component.html',
  styleUrls: ['./all-stock.component.css'],
})
export class AllStockComponent implements OnInit {
  title = 'All Stocks';
  requireLoader: boolean = true;
  allStocksData: RecommendationFive[] = [];
  allStocksTable: MatTableDataSource<RecommendationFive>;
  diplayedColumnsAllStocks: string[] = [
    'symbol',
    'currentPrice',
    'previousClose',
    'changePercentage',
    'change',
  ];
  allStockPageNo: number = 0;

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  constructor(
    private router: Router,
    private gainerLooserService: GainerLooserService,
  ) {}

  ngOnInit(): void {
    window.scrollTo(0, 0);
    this.gainerLooserService.getallStocks(this.allStockPageNo).subscribe((result) => {
      result.data.forEach((element) => {
        this.allStocksData.push({
          symbol: element.symbol,
          longName: element.longName,
          currentPrice: element.currentPrice.fmt,
          previousClose: element.previousClose.fmt,
          changePercentage: element.changePercentage.fmt,
          change: element.change.fmt,
        });
      });
      this.requireLoader = false;
      this.allStocksTable = new MatTableDataSource(this.allStocksData);
      console.log(this.allStocksTable);
    });
  }

  clickedRecommendation(row: any) {
    console.log(row.symbol);
    this.router.navigate(['/stockDetails/' + row.symbol]);
  }

  goBackAllStock = () => {
    this.requireLoader = true;
    this.allStocksTable = new MatTableDataSource();
    this.allStockPageNo -= 1;
    this.gainerLooserService.getallStocks(this.allStockPageNo).subscribe((result) => {
      this.allStocksData = [];
      result.data.forEach((element) => {
        this.allStocksData.push({
          symbol: element.symbol,
          longName: element.longName,
          currentPrice: element.currentPrice.fmt,
          previousClose: element.previousClose.fmt,
          changePercentage: element.changePercentage.fmt,
          change: element.change.fmt,
        });
      });
      this.requireLoader = false;
      console.log(this.allStocksData);
      this.allStocksTable = new MatTableDataSource(this.allStocksData);
    });
  };

  goToNextAllStock() {
    this.allStockPageNo += 1;
    this.requireLoader = true;
    this.allStocksTable = new MatTableDataSource();
    this.gainerLooserService.getallStocks(this.allStockPageNo).subscribe((result) => {
      this.allStocksData = [];
      result.data.forEach((element) => {
        this.allStocksData.push({
          symbol: element.symbol,
          longName: element.longName,
          currentPrice: element.currentPrice.fmt,
          previousClose: element.previousClose.fmt,
          changePercentage: element.changePercentage.fmt,
          change: element.change.fmt,
        });
      });
      this.requireLoader = false;
      console.log(this.allStocksData);
      this.allStocksTable = new MatTableDataSource(this.allStocksData);
    });
  }
}
