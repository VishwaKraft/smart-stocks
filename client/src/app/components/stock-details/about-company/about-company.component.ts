import { Component, OnInit } from '@angular/core';
import { StockDetailsService } from 'src/app/services/stock-details.service';
import { ActivatedRoute } from '@angular/router';
import { StockDetailsModel } from 'src/app/models/stockDetailsModel';

@Component({
  selector: 'app-about-company',
  templateUrl: './about-company.component.html',
  styleUrls: ['./about-company.component.css'],
})
export class AboutCompanyComponent implements OnInit {
  urlStockDetails: StockDetailsModel = new StockDetailsModel(
    '',
    '',
    '',
    '',
    '',
    '',
    { fmt: '' },
    { fmt: '' },
    { fmt: '' },
    { fmt: '' },
    { fmt: '' },
    { fmt: '' },
    { fmt: '' },
    { fmt: '' },
    { fmt: '' },
    { fmt: '' },
    { fmt: '' },
    '',
    { fmt: '' },
    '',
    '',
    { fmt: '' },
  );
  symbol: string = '';
  constructor(
    private stockDetailsService: StockDetailsService,
    private activatedRoute: ActivatedRoute,
  ) {}
  ngOnInit(): void {
    this.symbol = this.activatedRoute.snapshot.params['symbol'];
    this.stockDetailsService.fetchStockAbout(this.symbol).subscribe((res) => {
      this.urlStockDetails = res.data;
    });
  }
}
