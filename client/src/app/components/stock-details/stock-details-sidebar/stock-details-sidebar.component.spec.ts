import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StockDetailsSidebarComponent } from './stock-details-sidebar.component';

describe('StockDetailsSidebarComponent', () => {
  let component: StockDetailsSidebarComponent;
  let fixture: ComponentFixture<StockDetailsSidebarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [StockDetailsSidebarComponent],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(StockDetailsSidebarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
