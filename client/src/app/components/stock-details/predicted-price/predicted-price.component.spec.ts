import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PredictedPriceComponent } from './predicted-price.component';

describe('PredictedPriceComponent', () => {
  let component: PredictedPriceComponent;
  let fixture: ComponentFixture<PredictedPriceComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [PredictedPriceComponent],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(PredictedPriceComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
