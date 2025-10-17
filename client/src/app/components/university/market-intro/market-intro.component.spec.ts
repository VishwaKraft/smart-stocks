import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MarketIntroComponent } from './market-intro.component';

describe('MarketIntroComponent', () => {
  let component: MarketIntroComponent;
  let fixture: ComponentFixture<MarketIntroComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [MarketIntroComponent],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(MarketIntroComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
