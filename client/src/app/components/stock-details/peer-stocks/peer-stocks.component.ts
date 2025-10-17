import { Component, OnInit } from '@angular/core';
import { StockDetailsService } from 'src/app/services/stock-details.service';
import { ActivatedRoute, Router } from '@angular/router';
import { PeopleStock } from 'src/app/Interface/PeopleStock';
import { NGXLogger } from 'ngx-logger';

@Component({
  selector: 'app-peer-stocks',
  templateUrl: './peer-stocks.component.html',
  styleUrls: ['./peer-stocks.component.css'],
})
export class PeerStocksComponent implements OnInit {
  stockDetails: any;
  displayedColumns: string[] = [
    'companyName',
    'currentPrice',
    'previousClose',
    'change',
    'change%',
  ];
  dataSource: PeopleStock[] = [];
  symbol = '';
  constructor(
    private router: Router,
    private stockDetailsService: StockDetailsService,
    private activatedRoute: ActivatedRoute,
    private logger: NGXLogger,
  ) {}
  ngOnInit(): void {
    this.symbol = this.activatedRoute.snapshot.params['symbol'];
    this.logger.info(`Fetching peer stocks for symbol: ${this.symbol}`);
    this.stockDetailsService.fetchPeerStock(this.symbol).subscribe((res) => {
      this.dataSource = res.data;
      this.logger.info('Successfully fetched peer stocks.');
      this.logger.debug('Peer stocks data:', res.data);
    });
  }

  clickedRow(row: any) {
    this.logger.info(`User clicked to view new stock details: ${row.symbol}`);
    this.router.navigate(['/stockDetails/' + row.symbol]).then(() => {
      window.location.reload();
    });
  }
}
