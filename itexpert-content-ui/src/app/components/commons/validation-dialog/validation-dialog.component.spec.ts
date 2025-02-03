import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ValidationDialogComponent } from './validation-dialog.component';

describe('ValidationDialogComponent', () => {
  let component: ValidationDialogComponent;
  let fixture: ComponentFixture<ValidationDialogComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ValidationDialogComponent]
    });
    fixture = TestBed.createComponent(ValidationDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
