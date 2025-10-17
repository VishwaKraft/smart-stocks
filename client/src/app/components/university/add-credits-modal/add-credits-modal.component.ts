import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormArray, Validators, FormControl } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { DashboardService } from 'src/app/services/dashboard.service';

@Component({
  selector: 'app-add-credits-modal',
  templateUrl: './add-credits-modal.component.html',
  styleUrls: ['./add-credits-modal.component.css'],
})
export class AddCreditsModalComponent implements OnInit {
  AddCreditsForm: FormGroup = new FormGroup({});
  defaultCredits: number = 1000;
  defaultAmount: number = this.defaultCredits / 10;
  constructor(
    private _fb: FormBuilder,
    public dialogRef: MatDialogRef<AddCreditsModalComponent>,
    private dashboardService: DashboardService,
  ) {}

  ngOnInit(): void {
    this.AddCreditsForm = this._fb.group({
      noOfCredits: [this.defaultCredits, [Validators.required]],
      amount: [this.defaultAmount, [Validators.required]],
    });
  }

  onSubmit() {
    this.dashboardService.addCredits(this.AddCreditsForm.value.noOfCredits);
    this.dialogRef.close();
  }

  onCreditChange(value) {
    this.defaultAmount = value / 10;
  }
}
