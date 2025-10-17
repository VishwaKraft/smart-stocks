import { Component, OnInit, ViewChild } from '@angular/core';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { Router } from '@angular/router';
import { RecommendationFive } from 'src/app/Interface/RecommendationFive';
import { GainerLooserService } from 'src/app/services/gainer-looser.service';

@Component({
  selector: 'app-recommendation',
  templateUrl: './recommendation.component.html',
  styleUrls: ['./recommendation.component.css'],
})
export class RecommendationComponent implements OnInit {
  title = ' Top Recommendated Stock for Today';
  recommendationData: RecommendationFive[] = [];
  recommendationTable: MatTableDataSource<RecommendationFive>;
  displayedColumnsRecomd: string[] = [
    'longName',
    'currentPrice',
    'previousClose',
    'changePercentage',
    'change',
  ];

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;
  requireLoader: boolean = true;

  constructor(
    private router: Router,
    private gainerLooserService: GainerLooserService,
  ) {}

  ngOnInit(): void {
    window.scrollTo(0, 0);
    this.gainerLooserService.getTopLooser().subscribe((v) => {
      this.gainerLooserService
        .getTopRecommendation(v.data.stock.exploreCompanyList.TOP_LOSERS[0].stats.symbol + '.NS')
        .subscribe((value) => {
          value.data.forEach((element) => {
            this.recommendationData.push({
              symbol: element.symbol,
              longName: element.longName,
              currentPrice: element.currentPrice.fmt,
              previousClose: element.previousClose.fmt,
              changePercentage: element.changePercentage.fmt,
              change: element.change.fmt,
            });
          });
          this.requireLoader = false;
          this.recommendationTable = new MatTableDataSource(this.recommendationData);
          this.recommendationTable.paginator = this.paginator;
          this.recommendationTable.sort = this.sort;
        });
    });
  }

  clickedRecommendation(row: any) {
    console.log(row.symbol);
    this.router.navigate(['/stockDetails/' + row.symbol]);
  }
}
