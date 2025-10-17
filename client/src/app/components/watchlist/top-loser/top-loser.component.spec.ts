import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TopLoserComponent } from './top-loser.component';

describe('TopLoserComponent', () => {
  let component: TopLoserComponent;
  let fixture: ComponentFixture<TopLoserComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [TopLoserComponent],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(TopLoserComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
