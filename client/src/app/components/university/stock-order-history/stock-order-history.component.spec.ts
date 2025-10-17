import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StockOrderHistoryComponent } from './stock-order-history.component';

describe('StockOrderHistoryComponent', () => {
  let component: StockOrderHistoryComponent;
  let fixture: ComponentFixture<StockOrderHistoryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [StockOrderHistoryComponent],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(StockOrderHistoryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
