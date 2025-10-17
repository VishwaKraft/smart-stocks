import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TopGainerComponent } from './top-gainer.component';

describe('TopGainerComponent', () => {
  let component: TopGainerComponent;
  let fixture: ComponentFixture<TopGainerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [TopGainerComponent],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(TopGainerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
