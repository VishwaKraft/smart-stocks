import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AddCreditsModalComponent } from './add-credits-modal.component';

describe('AddCreditsModalComponent', () => {
  let component: AddCreditsModalComponent;
  let fixture: ComponentFixture<AddCreditsModalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [AddCreditsModalComponent],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(AddCreditsModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
