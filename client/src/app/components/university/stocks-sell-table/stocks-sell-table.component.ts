import { Component, OnDestroy, OnInit } from '@angular/core';
import { MatDialog, MatDialogConfig } from '@angular/material/dialog';
import { Subscription } from 'rxjs';
import { SellStocks } from 'src/app/Interface/SellStocks';
import { DashboardService } from 'src/app/services/dashboard.service';
import {
  computeLineValue,
  getStockStatus,
  getStockStatusLabel,
  parseUnits,
  StockStatus,
} from 'src/app/utils/inventory.utils';
import { BuySellModalComponent } from '../buy-sell-modal/buy-sell-modal.component';

type InventoryFilter = 'all' | 'low-stock' | 'out-of-stock';

interface InventoryItem extends SellStocks {
  lineValue: number;
  status: StockStatus;
  statusLabel: string;
}

@Component({
  selector: 'app-stocks-sell-table',
  templateUrl: './stocks-sell-table.component.html',
  styleUrls: ['./stocks-sell-table.component.css']
})
export class StocksSellTableComponent implements OnInit, OnDestroy {
  displayedColumns: string[] = [
    'stockName',
    'noOfUnits',
    'price',
    'lineValue',
    'status',
    'sell'
  ];
  dataSource: InventoryItem[] = [];
  filteredData: InventoryItem[] = [];
  activeFilter: InventoryFilter = 'all';
  subscription: Subscription;

  constructor(
    private dashboardService: DashboardService,
    private dialog: MatDialog,
  ) {}

  ngOnInit(): void {
    this.loadInventory();

    this.subscription = this.dashboardService.dataChanged.subscribe(() => {
      this.loadInventory();
    });
  }

  private loadInventory(): void {
    this.dashboardService.fetchCurrentStocks().subscribe((res) => {
      this.dataSource = res.data.map((item) => this.enrichItem(item));
      this.applyFilter();
    });
  }

  private enrichItem(item: SellStocks): InventoryItem {
    const status = getStockStatus(parseUnits(item.units));
    return {
      ...item,
      lineValue: computeLineValue(item.units, item.price),
      status,
      statusLabel: getStockStatusLabel(status),
    };
  }

  setFilter(filter: InventoryFilter): void {
    this.activeFilter = filter;
    this.applyFilter();
  }

  private applyFilter(): void {
    switch (this.activeFilter) {
      case 'low-stock':
        this.filteredData = this.dataSource.filter((i) => i.status === 'low-stock');
        break;
      case 'out-of-stock':
        this.filteredData = this.dataSource.filter((i) => i.status === 'out-of-stock');
        break;
      default:
        this.filteredData = [...this.dataSource];
    }
  }

  get lowStockCount(): number {
    return this.dataSource.filter((i) => i.status === 'low-stock').length;
  }

  stockOut(item: InventoryItem): void {
    this.dashboardService.setStock({ type: 'Stock Out', name: item.symbol, currPrice: item.price });
    const dialogConfig = new MatDialogConfig();
    dialogConfig.autoFocus = true;
    dialogConfig.width = '30%';
    this.dialog.open(BuySellModalComponent, dialogConfig);
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }
}
