import { Component, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { StockOrder } from 'src/app/Interface/StockOrder';
import { DashboardService } from 'src/app/services/dashboard.service';

@Component({
  selector: 'app-stock-order-history',
  templateUrl: './stock-order-history.component.html',
  styleUrls: ['./stock-order-history.component.css'],
})
export class StockOrderHistoryComponent implements OnInit {
  displayedColumns: string[] = [
    'date_time',
    'stockName',
    'noOfUnits',
    'type',
    'price',
    'profit_loss',
  ];
  dataSource: StockOrder[] = [];
  subscription: Subscription;
  constructor(private dashboardService: DashboardService) {}

  ngOnInit(): void {
    window.scrollTo(0, 0);
    this.dashboardService.fetchStockOrderHistory().subscribe((res) => {
      this.dataSource = res.data;
      for (var i = 0; i < this.dataSource.length; i++) {
        var date = new Date(this.dataSource[i].transactionsDate);
        date = new Date(date.getTime() + 330 * 60000);
        this.dataSource[i].transactionsDate = date.toLocaleString('en-IN', {
          timeZone: 'Asia/Kolkata',
          hour12: true,
        });

        if (this.dataSource[i].type == 'BUY') {
          this.dataSource[i].profit = '-';
        } else if (this.dataSource[i].type == 'SELL') {
          this.dataSource[i].profit = parseFloat(this.dataSource[i].profit).toFixed(2).toString();
        }
      }
    });

    this.subscription = this.dashboardService.dataChanged.subscribe(() => {
      this.dashboardService.fetchStockOrderHistory().subscribe((res) => {
        this.dataSource = res.data;
        for (var i = 0; i < this.dataSource.length; i++) {
          var date = new Date(this.dataSource[i].transactionsDate);
          date = new Date(date.getTime() + 330 * 60000);
          this.dataSource[i].transactionsDate = date.toLocaleString('en-IN', {
            timeZone: 'Asia/Kolkata',
            hour12: true,
          });

          if (this.dataSource[i].type == 'BUY') {
            this.dataSource[i].profit = '-';
          } else if (this.dataSource[i].type == 'SELL') {
            this.dataSource[i].profit = parseFloat(this.dataSource[i].profit).toFixed(2).toString();
          }
        }
      });
    });
  }
}
