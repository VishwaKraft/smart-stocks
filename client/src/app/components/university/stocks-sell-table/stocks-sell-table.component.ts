import { Component, OnInit } from '@angular/core';
import { MatDialog, MatDialogConfig } from '@angular/material/dialog';
import { Subscription } from 'rxjs';
import { SellStocks } from 'src/app/Interface/SellStocks';
import { DashboardService } from 'src/app/services/dashboard.service';
import { BuySellModalComponent } from '../buy-sell-modal/buy-sell-modal.component';

@Component({
  selector: 'app-stocks-sell-table',
  templateUrl: './stocks-sell-table.component.html',
  styleUrls: ['./stocks-sell-table.component.css'],
})
export class StocksSellTableComponent implements OnInit {
  displayedColumns: string[] = ['stockName', 'noOfUnits', 'price', 'sell'];
  dataSource: SellStocks[] = [];
  subscription: Subscription;
  refresh;
  constructor(
    private dashboardService: DashboardService,
    private dialog: MatDialog,
  ) {}

  ngOnInit(): void {
    this.dashboardService.fetchCurrentStocks().subscribe((res) => {
      this.dataSource = res.data;
    });

    this.subscription = this.dashboardService.dataChanged.subscribe(() => {
      this.dashboardService.fetchCurrentStocks().subscribe((res) => {
        this.dataSource = res.data;
      });
    });
  }

  sellStock(stock: SellStocks) {
    this.dashboardService.setStock({ type: 'Sell', name: stock.symbol, currPrice: stock.price });
    const dialogConfig = new MatDialogConfig();
    dialogConfig.autoFocus = true;
    dialogConfig.width = '30%';
    this.dialog.open(BuySellModalComponent, dialogConfig);
  }

  ngOnDestroy() {
    if (this.refresh) {
      clearInterval(this.refresh);
    }
  }
}
