import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StocksSellTableComponent } from './stocks-sell-table.component';

describe('StocksSellTableComponent', () => {
  let component: StocksSellTableComponent;
  let fixture: ComponentFixture<StocksSellTableComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [StocksSellTableComponent],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(StocksSellTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
