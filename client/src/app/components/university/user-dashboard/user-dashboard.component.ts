import { HttpClient } from '@angular/common/http';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { MatDialog, MatDialogConfig } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { SellStocks } from 'src/app/Interface/SellStocks';
import { DashboardService } from 'src/app/services/dashboard.service';
import { computeInventorySummary, InventorySummary } from 'src/app/utils/inventory.utils';
import { AddCreditsModalComponent } from '../add-credits-modal/add-credits-modal.component';

@Component({
  selector: 'app-user-dashboard',
  templateUrl: './user-dashboard.component.html',
  styleUrls: ['./user-dashboard.component.css'],
})
export class UserDashboardComponent implements OnInit, OnDestroy {
  availableBudget: string;
  inventorySummary: InventorySummary = {
    totalValue: 0,
    skuCount: 0,
    lowStockCount: 0,
    outOfStockCount: 0,
    totalUnits: 0,
  };
  inventoryItems: SellStocks[] = [];
  subscription: Subscription;
  refresh: ReturnType<typeof setInterval>;

  constructor(
    private http: HttpClient,
    private dashboardService: DashboardService,
    private dialog: MatDialog,
    private router: Router
  ) {}

  ngOnInit(): void {
    window.scrollTo(0, 0);
    this.loadDashboardData();

    this.refresh = setInterval(() => this.loadDashboardData(), 5000);

    this.subscription = this.dashboardService.dataChanged.subscribe(() => {
      this.loadDashboardData();
    });
  }

  private loadDashboardData(): void {
    this.dashboardService.fetchUserCredit().subscribe((res) => {
      this.availableBudget = res.data.money.toFixed(2);
    });

    this.dashboardService.fetchCurrentStocks().subscribe((res) => {
      this.inventoryItems = res.data;
      this.inventorySummary = computeInventorySummary(res.data);
    });
  }

  addBudget(): void {
    const dialogConfig = new MatDialogConfig();
    dialogConfig.autoFocus = true;
    dialogConfig.width = '30%';
    this.dialog.open(AddCreditsModalComponent, dialogConfig);
  }

  viewMovements(): void {
    this.router.navigateByUrl('/user/sandbox/orders');
  }

  openCatalog(): void {
    this.router.navigateByUrl('/user/sandbox/stocks');
  }

  ngOnDestroy(): void {
    if (this.refresh) {
      clearInterval(this.refresh);
    }
    this.subscription?.unsubscribe();
  }
}
