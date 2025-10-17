import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormArray, Validators, FormControl } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { DashboardService } from 'src/app/services/dashboard.service';

@Component({
  selector: 'app-buy-sell-modal',
  templateUrl: './buy-sell-modal.component.html',
  styleUrls: ['./buy-sell-modal.component.css'],
})
export class BuySellModalComponent implements OnInit {
  BuySellForm: FormGroup = new FormGroup({});
  type: string;
  stockName: string = '';
  stockPrice: string = '';
  stock: { type: string; name: string; currPrice: string };
  constructor(
    private _fb: FormBuilder,
    public dialogRef: MatDialogRef<BuySellModalComponent>,
    private dashboardService: DashboardService,
  ) {}

  ngOnInit(): void {
    this.stock = this.dashboardService.getStock();
    this.BuySellForm = this._fb.group({
      name: [this.stock.name],
      currPrice: [this.stock.currPrice],
      noOfUnits: [null, [Validators.required]],
    });
  }

  onSubmit() {
    if (this.stock.type == 'Buy') {
      this.dashboardService.buyStock({
        symbol: this.BuySellForm.value.name,
        units: this.BuySellForm.value.noOfUnits,
      });
    } else if (this.stock.type == 'Sell') {
      this.dashboardService.sellStock({
        symbol: this.BuySellForm.value.name,
        units: this.BuySellForm.value.noOfUnits,
      });
    }
    this.dialogRef.close();
  }
}
