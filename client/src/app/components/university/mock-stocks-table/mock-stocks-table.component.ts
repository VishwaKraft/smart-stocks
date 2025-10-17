import { Component, OnInit } from '@angular/core';
import { MockStock } from 'src/app/Interface/MockStock';
import { BuySellModalComponent } from '../buy-sell-modal/buy-sell-modal.component';
import { MatDialog, MatDialogConfig } from '@angular/material/dialog';
import { DashboardService } from 'src/app/services/dashboard.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-mock-stocks-table',
  templateUrl: './mock-stocks-table.component.html',
  styleUrls: ['./mock-stocks-table.component.css'],
})
export class MockStocksTableComponent implements OnInit {
  displayedColumns: string[] = ['stockName', 'currentPrice', 'previousClose', 'high', 'low', 'buy'];
  dataSource: MockStock[] = [];
  subscription: Subscription;
  id;
  constructor(
    private dashboardService: DashboardService,
    private dialog: MatDialog,
  ) {}

  ngOnInit(): void {
    window.scrollTo(0, 0);
    this.dashboardService.fetchMockStocks().subscribe((res) => {
      this.dataSource = res.data;
    });

    this.id = setInterval(() => {
      this.dashboardService.fetchMockStocks().subscribe((res) => {
        this.dataSource = res.data;
      });
    }, 3000);
  }

  buyStock(stock: MockStock) {
    this.dashboardService.setStock({
      type: 'Buy',
      name: stock.companyName,
      currPrice: stock.price,
    });
    const dialogConfig = new MatDialogConfig();
    dialogConfig.autoFocus = true;
    dialogConfig.width = '30%';
    this.dialog.open(BuySellModalComponent, dialogConfig);
  }

  ngOnDestroy() {
    if (this.id) {
      clearInterval(this.id);
    }
  }
}
