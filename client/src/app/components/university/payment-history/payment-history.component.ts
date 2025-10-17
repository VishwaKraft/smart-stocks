import { Component, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { PaymentHistory } from 'src/app/Interface/PaymentHistory';
import { DashboardService } from 'src/app/services/dashboard.service';

@Component({
  selector: 'app-payment-history',
  templateUrl: './payment-history.component.html',
  styleUrls: ['./payment-history.component.css'],
})
export class PaymentHistoryComponent implements OnInit {
  paymentHistory: any;
  displayedColumns: string[] = ['transactionsDate', 'noOfCredits', 'amount', 'type', 'status'];
  dataSource: PaymentHistory[] = [];
  subscription: Subscription;
  constructor(private dashboardService: DashboardService) {}

  ngOnInit(): void {
    window.scrollTo(0, 0);
    this.dashboardService.fetchPaymentHistory().subscribe((res) => {
      this.dataSource = res.data;
      for (var i = 0; i < this.dataSource.length; i++) {
        var date = new Date(this.dataSource[i].transactionsDate);
        date = new Date(date.getTime() + 330 * 60000);
        this.dataSource[i].transactionsDate = date.toLocaleString('en-IN');
      }
    });

    this.subscription = this.dashboardService.dataChanged.subscribe(() => {
      this.dashboardService.fetchPaymentHistory().subscribe((res) => {
        this.dataSource = res.data;
        for (var i = 0; i < this.dataSource.length; i++) {
          var date = new Date(this.dataSource[i].transactionsDate);
          date = new Date(date.getTime() + 330 * 60000);
          this.dataSource[i].transactionsDate = date.toLocaleString('en-IN');
        }
      });
    });
  }
}
