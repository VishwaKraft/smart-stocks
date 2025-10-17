import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MockStocksTableComponent } from './mock-stocks-table.component';

describe('MockStocksTableComponent', () => {
  let component: MockStocksTableComponent;
  let fixture: ComponentFixture<MockStocksTableComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [MockStocksTableComponent],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(MockStocksTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
