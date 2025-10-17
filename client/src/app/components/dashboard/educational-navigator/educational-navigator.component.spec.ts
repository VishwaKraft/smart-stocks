import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EducationalNavigatorComponent } from './educational-navigator.component';

describe('EducationalNavigatorComponent', () => {
  let component: EducationalNavigatorComponent;
  let fixture: ComponentFixture<EducationalNavigatorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [EducationalNavigatorComponent],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(EducationalNavigatorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
