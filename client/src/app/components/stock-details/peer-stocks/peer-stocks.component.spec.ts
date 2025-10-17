import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PeerStocksComponent } from './peer-stocks.component';

describe('PeerStocksComponent', () => {
  let component: PeerStocksComponent;
  let fixture: ComponentFixture<PeerStocksComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [PeerStocksComponent],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(PeerStocksComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
